package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.FinTrackerApp
import com.example.capture.PaymentNotificationListenerService
import com.example.capture.SmsContentObserverService
import com.example.capture.SmsInboxScanReport
import com.example.capture.SmsObserverStatus
import com.example.capture.SmsTransactionParser
import com.example.data.CategoryBudgetEntity
import com.example.data.MonthlyBudgetEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionSource
import com.example.data.UserPrefs
import com.example.domain.ExpenseCategory
import com.example.domain.InrFormatter
import com.example.export.ExportManager
import com.example.ml.MlPrediction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class FinTrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FinTrackerApp
    private val repository = app.repository
    private val monthKey = InrFormatter.currentMonthKey()
    private val simulationCursor = AtomicInteger(0)

    private val isScanningSms = MutableStateFlow(false)
    private val lastScanReport = MutableStateFlow<SmsInboxScanReport?>(null)
    private val parserLabInput = MutableStateFlow(SmsTransactionParser.testPresets.first().rawMessage)
    private val parserLabTrace = MutableStateFlow(
        repository.inspectMessageDryRun(SmsTransactionParser.testPresets.first().rawMessage),
    )
    private val statusMessage = MutableStateFlow<String?>(null)
    private val dismissMonthlyPromptOverride = MutableStateFlow(false)
    private val permissionTick = MutableStateFlow(0)
    private val suggestedGoal = MutableStateFlow(50_000.0)

    private data class AuxiliaryState(
        val isScanning: Boolean,
        val scanReport: SmsInboxScanReport?,
        val labInput: String,
        val labTrace: com.example.capture.SmsParseTrace?,
        val status: String?,
        val dismissedPrompt: Boolean,
        val permTick: Int,
        val suggestedGoalInr: Double,
        val observerStatus: SmsObserverStatus,
    )

    private val auxiliaryFlow = combine(
        isScanningSms,
        lastScanReport,
        parserLabInput,
        parserLabTrace,
        statusMessage,
    ) { scanning, report, labIn, labTr, msg ->
        AuxiliaryState(
            isScanning = scanning,
            scanReport = report,
            labInput = labIn,
            labTrace = labTr,
            status = msg,
            dismissedPrompt = false,
            permTick = 0,
            suggestedGoalInr = 50_000.0,
            observerStatus = SmsObserverStatus(),
        )
    }.combine(
        combine(
            dismissMonthlyPromptOverride,
            permissionTick,
            suggestedGoal,
            SmsContentObserverService.observerStatus,
        ) { d, p, s, obs ->
             listOf(d, p, s, obs)
        },
    ) { aux, extra ->
        aux.copy(
            dismissedPrompt = extra[0] as Boolean,
            permTick = extra[1] as Int,
            suggestedGoalInr = extra[2] as Double,
            observerStatus = extra[3] as SmsObserverStatus,
        )
    }

    val uiState: StateFlow<FinTrackerUiState> = combine(
        repository.observeMonthTransactions(monthKey),
        repository.observeMonthlyBudget(monthKey),
        repository.observeCategoryBudgets(monthKey),
        repository.prefs.prefsFlow,
        auxiliaryFlow,
    ) { transactions, monthlyBudget, categoryBudgets, prefs, aux ->
        buildUiState(transactions, monthlyBudget, categoryBudgets, prefs, aux)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinTrackerUiState(
            monthKey = monthKey,
            monthTitle = InrFormatter.formatMonthTitle(monthKey),
            parserLabInput = parserLabInput.value,
            parserLabTrace = parserLabTrace.value,
        ),
    )

    init {
        SmsContentObserverService.start(app)
        viewModelScope.launch {
            repository.hydrateMlWeights()
            val latest = repository.getLatestMonthlyBudget()
            if (latest != null && latest.totalGoalInr > 0) {
                suggestedGoal.value = latest.totalGoalInr
            }
            if (repository.hasReadSmsPermission()) {
                scanSmsInbox(silentIfEmpty = true)
            }
        }
    }

    fun refreshPermissionsState() {
        permissionTick.value += 1
        SmsContentObserverService.start(app)
    }

    fun onSmsPermissionResult(granted: Boolean) {
        permissionTick.value += 1
        SmsContentObserverService.start(app)
        if (granted) {
            scanSmsInbox(silentIfEmpty = false)
        } else {
            statusMessage.value = if (BuildConfig.ENABLE_TESTING_TOOLS) {
                "SMS permission declined — use Simulate Bank SMS or Parser Lab to test."
            } else {
                "SMS permission declined — enable SMS permission in Settings or add expenses manually via +."
            }
        }
    }

    fun scanSmsInbox(silentIfEmpty: Boolean = false) {
        if (isScanningSms.value) return
        viewModelScope.launch {
            isScanningSms.value = true
            val report = repository.scanSmsInbox(monthKey)
            lastScanReport.value = report
            isScanningSms.value = false
            if (!silentIfEmpty || report.newlyInsertedCount > 0) {
                statusMessage.value = when {
                    !report.permissionGranted -> if (BuildConfig.ENABLE_TESTING_TOOLS) {
                        "Grant SMS permission or tap 'Simulate SMS' to test live parsing."
                    } else {
                        "Grant SMS permission in Android Settings to scan bank & UPI messages."
                    }
                    report.newlyInsertedCount > 0 ->
                        "Captured ${report.newlyInsertedCount} new expense${if (report.newlyInsertedCount == 1) "" else "s"} from ${report.inspectedCount} SMS."
                    else ->
                        "Scanned ${report.inspectedCount} SMS · ${report.matchedCount} matched · ${report.skippedCount} OTP/promo skipped."
                }
            }
        }
    }

    /**
     * Fires [count] realistic Indian bank/UPI SMS messages through SmsContentObserverService
     * and the regex parser so the user can test any number of incoming SMS messages on the emulator.
     */
    fun simulateIncomingBankSmsBatch(count: Int = 3) {
        if (!BuildConfig.ENABLE_TESTING_TOOLS) return
        viewModelScope.launch {
            val validPresets = SmsTransactionParser.testPresets.filter { !it.isExpectedReject }
            var addedCount = 0
            val baseNow = System.currentTimeMillis()

            for (i in 0 until count) {
                val idx = simulationCursor.getAndIncrement() % validPresets.size
                val preset = validPresets[idx]
                // Add slight rupee variation so repeated simulations produce distinct realistic transactions
                val jitter = (simulationCursor.get() * 15) % 190
                val amountRegex = Regex("""([0-9][0-9,]*(?:\.[0-9]{1,2})?)""")
                val firstNumMatch = amountRegex.find(preset.rawMessage)
                val messageWithVariation = if (firstNumMatch != null) {
                    val baseVal = firstNumMatch.value.replace(",", "").toDoubleOrNull() ?: 250.0
                    val updatedAmt = String.format(Locale.US, "%.2f", baseVal + jitter)
                    preset.rawMessage.replaceFirst(firstNumMatch.value, updatedAmt)
                } else {
                    preset.rawMessage
                }

                // Trigger SmsContentObserverService so ContentObserver telemetry updates live
                SmsContentObserverService.simulateIncomingSms(
                    context = app,
                    sender = preset.sender,
                    rawMessage = messageWithVariation,
                )

                // Also ensure immediate ingestion with staggered timestamps for smooth UI animation
                val trace = repository.ingestIncomingMessage(
                    rawMessage = messageWithVariation,
                    sender = preset.sender,
                    timestampMillis = baseNow - (i * 240_000L) - (simulationCursor.get() * 60_000L),
                    source = TransactionSource.SMS_LIVE,
                )
                if (trace.accepted) addedCount++
                if (count > 1) delay(120)
            }

            statusMessage.value = "Simulated $addedCount incoming Indian bank SMS alert${if (addedCount == 1) "" else "s"} via ContentObserver"
        }
    }

    fun saveMonthlyGoal(goalInr: Double, customLimits: Map<ExpenseCategory, Double>? = null) {
        if (goalInr <= 0) return
        dismissMonthlyPromptOverride.value = true
        viewModelScope.launch {
            repository.saveMonthlyGoal(monthKey, goalInr, customLimits)
            statusMessage.value = "Monthly goal set to ${InrFormatter.format(goalInr)}"
        }
    }

    fun dismissMonthlyGoalPrompt() {
        dismissMonthlyPromptOverride.value = true
        viewModelScope.launch {
            repository.prefs.setLastPromptedMonth(monthKey)
        }
    }

    fun reopenMonthlyGoalPrompt() {
        dismissMonthlyPromptOverride.value = false
    }

    fun updateCategoryBudget(category: ExpenseCategory, limitInr: Double) {
        if (limitInr <= 0) return
        viewModelScope.launch {
            repository.updateCategoryLimit(monthKey, category, limitInr)
            statusMessage.value = "${category.shortName} budget updated to ${InrFormatter.format(limitInr)}"
        }
    }

    fun updateUserDisplayName(name: String) {
        viewModelScope.launch {
            repository.prefs.setUserDisplayName(name)
        }
    }

    fun updateParserLabInput(rawText: String) {
        parserLabInput.value = rawText
        parserLabTrace.value = repository.inspectMessageDryRun(rawText)
    }

    fun saveParserLabTraceToLedger() {
        val raw = parserLabInput.value
        val trace = parserLabTrace.value ?: return
        if (!trace.accepted) return
        viewModelScope.launch {
            SmsContentObserverService.simulateIncomingSms(app, "PARSER-LAB", raw)
            repository.ingestIncomingMessage(
                rawMessage = raw,
                sender = "ParserLab",
                timestampMillis = System.currentTimeMillis(),
                source = TransactionSource.SMART_PASTE,
            )
            statusMessage.value = "Saved ${trace.merchant} (${InrFormatter.format(trace.amountInr ?: 0.0)}) to ledger"
        }
    }

    fun handleIncomingSharedText(sharedText: String) {
        parserLabInput.value = sharedText
        val trace = repository.inspectMessageDryRun(sharedText)
        parserLabTrace.value = trace
        if (trace.accepted) {
            viewModelScope.launch {
                repository.ingestIncomingMessage(
                    rawMessage = sharedText,
                    sender = "SharedAlert",
                    timestampMillis = System.currentTimeMillis(),
                    source = TransactionSource.SMART_PASTE,
                )
                statusMessage.value = "Parsed shared alert: ${trace.merchant} (${InrFormatter.format(trace.amountInr ?: 0.0)})"
            }
        } else {
            statusMessage.value = trace.rejectionReason ?: "Could not extract an expense from shared text."
        }
    }

    fun previewMlCategory(merchant: String, note: String = ""): MlPrediction =
        repository.classifier.classify(merchant, note)

    fun addQuickExpense(
        amountInr: Double,
        merchant: String,
        categoryOverride: ExpenseCategory?,
        note: String,
    ) {
        if (amountInr <= 0 || merchant.isBlank()) return
        viewModelScope.launch {
            repository.addManualOrQuickExpense(
                amountInr = amountInr,
                merchant = merchant,
                categoryOverride = categoryOverride,
                note = note,
                source = TransactionSource.MANUAL,
            )
            statusMessage.value = "Logged ${InrFormatter.format(amountInr)} at ${merchant.trim()}"
        }
    }

    fun recategorizeTransaction(tx: TransactionEntity, newCategory: ExpenseCategory) {
        viewModelScope.launch {
            repository.recategorizeTransaction(tx, newCategory)
            statusMessage.value = "Updated to ${newCategory.shortName} · On-device ML learned from this edit"
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            statusMessage.value = "Transaction removed"
        }
    }

    fun importCsvFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val lines = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.readLines().orEmpty()
            }
            val count = repository.importCsvStatement(lines)
            statusMessage.value = if (count > 0) {
                "Imported $count transactions locally from CSV"
            } else {
                "No new valid transactions found in CSV"
            }
        }
    }

    fun exportCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            val current = uiState.value.transactions
            val result = ExportManager.writeCsv(context, uri, current)
            statusMessage.value = result.fold(
                onSuccess = { count -> "Exported $count transactions to CSV" },
                onFailure = { "CSV export failed: ${it.message}" },
            )
        }
    }

    fun exportPdf(context: Context, uri: Uri) {
        viewModelScope.launch {
            val state = uiState.value
            val result = ExportManager.writePdf(
                context = context,
                uri = uri,
                monthTitle = state.monthTitle,
                monthlyGoalInr = state.monthlyGoalInr.takeIf { state.hasMonthlyGoal },
                transactions = state.transactions,
            )
            statusMessage.value = result.fold(
                onSuccess = { count -> "Exported PDF report ($count items)" },
                onFailure = { "PDF export failed: ${it.message}" },
            )
        }
    }

    fun clearAllLocalData() {
        viewModelScope.launch {
            repository.clearAllTransactions()
            statusMessage.value = "All local transactions cleared"
        }
    }

    fun clearStatusBanner() {
        statusMessage.value = null
    }

    private fun buildUiState(
        transactions: List<TransactionEntity>,
        monthlyBudget: MonthlyBudgetEntity?,
        categoryBudgets: List<CategoryBudgetEntity>,
        prefs: UserPrefs,
        aux: AuxiliaryState,
    ): FinTrackerUiState {
        val hasGoal = monthlyBudget != null && monthlyBudget.totalGoalInr > 0.0
        val goalAmount = monthlyBudget?.totalGoalInr ?: 0.0
        val totalSpent = transactions.sumOf { it.amountInr }
        val remaining = (goalAmount - totalSpent)
        val overallProgress = if (goalAmount > 0) (totalSpent / goalAmount).toFloat() else 0f

        val cal = Calendar.getInstance()
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val todayDay = cal.get(Calendar.DAY_OF_MONTH)
        val daysLeft = (daysInMonth - todayDay + 1).coerceAtLeast(1)
        val safeDaily = if (hasGoal) (remaining.coerceAtLeast(0.0) / daysLeft) else 0.0
        val targetDailyPace = if (hasGoal) (goalAmount / daysInMonth) else 0.0

        val budgetByCat = categoryBudgets.associate {
            ExpenseCategory.fromName(it.categoryName) to it.limitInr
        }

        val grouped = transactions.groupBy { ExpenseCategory.fromName(it.categoryName) }
        val summaries = ExpenseCategory.entries.map { cat ->
            val list = grouped[cat].orEmpty()
            val spent = list.sumOf { it.amountInr }
            val limit = budgetByCat[cat] ?: (goalAmount * cat.defaultBudgetRatio)
            val share = if (totalSpent > 0.0) (spent / totalSpent).toFloat() else 0f
            val progress = if (limit > 0.0) (spent / limit).toFloat() else share
            val topMerchants = list.groupBy { it.merchant }
                .map { (m, txs) -> m to txs.sumOf { it.amountInr } }
                .sortedByDescending { it.second }
                .take(4)

            CategorySummary(
                category = cat,
                spentInr = spent,
                budgetLimitInr = limit,
                transactionCount = list.size,
                shareOfTotal = share,
                budgetProgress = progress,
                topMerchants = topMerchants,
            )
        }.sortedByDescending { it.spentInr }

        val dayMap = HashMap<Int, Double>()
        val txCal = Calendar.getInstance()
        transactions.forEach { tx ->
            txCal.timeInMillis = tx.timestamp
            val dom = txCal.get(Calendar.DAY_OF_MONTH)
            dayMap[dom] = (dayMap[dom] ?: 0.0) + tx.amountInr
        }
        val windowStart = (todayDay - 13).coerceAtLeast(1)
        val dailySeries = (windowStart..todayDay).map { day ->
            val amt = dayMap[day] ?: 0.0
            DailySpendPoint(
                dayOfMonth = day,
                label = day.toString(),
                amountInr = amt,
                isOverPace = targetDailyPace > 0 && amt > targetDailyPace * 1.2,
            )
        }

        val dayTotalsByKey = transactions.groupBy { InrFormatter.formatDayKey(it.timestamp) }
            .mapValues { (_, list) -> list.sumOf { it.amountInr } }
        val dayLetterFmt = SimpleDateFormat("EEEEE", Locale.ENGLISH)
        val streakDots = (6 downTo 0).map { daysAgo ->
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            val key = InrFormatter.formatDayKey(c.timeInMillis)
            val spent = dayTotalsByKey[key] ?: 0.0
            val hasActivity = dayTotalsByKey.containsKey(key)
            val onPace = if (targetDailyPace > 0) spent <= targetDailyPace * 1.25 else hasActivity
            StreakDayDot(
                label = dayLetterFmt.format(Date(c.timeInMillis)),
                spentInr = spent,
                onPace = onPace,
                hasActivity = hasActivity,
            )
        }

        val streakCount = if (!hasGoal && transactions.isEmpty()) {
            0
        } else {
            var count = 0
            for (dot in streakDots.reversed()) {
                if (dot.onPace && (dot.hasActivity || hasGoal)) {
                    count++
                } else {
                    break
                }
            }
            count.coerceAtLeast(if (hasGoal) 1 else 0)
        }

        val shouldPromptGoal = !aux.dismissedPrompt &&
            (!hasGoal || prefs.lastPromptedMonth != monthKey)

        return FinTrackerUiState(
            monthKey = monthKey,
            monthTitle = InrFormatter.formatMonthTitle(monthKey),
            monthlyGoalInr = goalAmount,
            hasMonthlyGoal = hasGoal,
            showMonthlyGoalPrompt = shouldPromptGoal,
            suggestedGoalInr = aux.suggestedGoalInr,
            totalSpentInr = totalSpent,
            remainingInr = remaining,
            safeDailySpendInr = safeDaily,
            overallProgress = overallProgress,
            transactions = transactions,
            categorySummaries = summaries,
            dailySeries = dailySeries,
            streakCount = streakCount,
            streakDots = streakDots,
            prefs = prefs,
            hasSmsPermission = repository.hasReadSmsPermission(),
            isNotificationListenerEnabled = PaymentNotificationListenerService.isEnabled(app),
            smsObserverStatus = aux.observerStatus,
            isScanningSms = aux.isScanning,
            lastScanReport = aux.scanReport,
            parserLabInput = aux.labInput,
            parserLabTrace = aux.labTrace,
            statusBannerMessage = aux.status,
        )
    }
}
