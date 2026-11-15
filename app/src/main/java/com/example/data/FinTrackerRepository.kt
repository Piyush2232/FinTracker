package com.example.data

import android.content.Context
import com.example.capture.SmsInboxScanReport
import com.example.capture.SmsInboxScanner
import com.example.capture.SmsParseTrace
import com.example.capture.SmsTransactionParser
import com.example.domain.ExpenseCategory
import com.example.domain.InrFormatter
import com.example.ml.DiasporaMlClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FinTrackerRepository(
    private val context: Context,
    private val db: AppDatabase,
    val prefs: UserPreferencesRepository,
    val classifier: DiasporaMlClassifier,
) {
    private val transactionDao = db.transactionDao()
    private val budgetDao = db.budgetDao()
    private val mlWeightDao = db.mlWeightDao()
    private val smsScanner = SmsInboxScanner(context, classifier)

    suspend fun hydrateMlWeights() = withContext(Dispatchers.IO) {
        classifier.hydrateLearnedWeights(mlWeightDao.getAllWeights())
    }

    fun observeMonthTransactions(monthKey: String): Flow<List<TransactionEntity>> {
        val (start, end) = InrFormatter.monthBoundsMillis(monthKey)
        return transactionDao.observeBetween(start, end)
    }

    fun observeAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.observeAll()

    fun observeMonthlyBudget(monthKey: String): Flow<MonthlyBudgetEntity?> =
        budgetDao.observeMonthlyBudget(monthKey)

    fun observeCategoryBudgets(monthKey: String): Flow<List<CategoryBudgetEntity>> =
        budgetDao.observeCategoryBudgets(monthKey)

    suspend fun getLatestMonthlyBudget(): MonthlyBudgetEntity? =
        budgetDao.getLatestMonthlyBudget()

    suspend fun saveMonthlyGoal(
        monthKey: String,
        totalGoalInr: Double,
        customCategoryLimits: Map<ExpenseCategory, Double>? = null,
    ) = withContext(Dispatchers.IO) {
        budgetDao.upsertMonthlyBudget(
            MonthlyBudgetEntity(
                monthKey = monthKey,
                totalGoalInr = totalGoalInr,
            ),
        )
        val catBudgets = ExpenseCategory.entries.map { cat ->
            val limit = customCategoryLimits?.get(cat) ?: (totalGoalInr * cat.defaultBudgetRatio)
            CategoryBudgetEntity(
                monthKey = monthKey,
                categoryName = cat.name,
                limitInr = limit,
            )
        }
        budgetDao.upsertCategoryBudgets(catBudgets)
        prefs.setLastPromptedMonth(monthKey)
    }

    suspend fun updateCategoryLimit(
        monthKey: String,
        category: ExpenseCategory,
        limitInr: Double,
    ) = withContext(Dispatchers.IO) {
        budgetDao.upsertCategoryBudgets(
            listOf(
                CategoryBudgetEntity(
                    monthKey = monthKey,
                    categoryName = category.name,
                    limitInr = limitInr,
                ),
            ),
        )
    }

    fun hasReadSmsPermission(): Boolean = smsScanner.hasReadSmsPermission()

    suspend fun scanSmsInbox(monthKey: String = InrFormatter.currentMonthKey()): SmsInboxScanReport =
        withContext(Dispatchers.IO) {
            hydrateMlWeights()
            val (report, entities) = smsScanner.scanCurrentMonthInbox(monthKey)
            var newlyInserted = 0
            if (entities.isNotEmpty()) {
                val results = transactionDao.insertAllIgnore(entities)
                newlyInserted = results.count { it != -1L }
            }
            if (report.permissionGranted) {
                prefs.recordSmsScanDiagnostics(
                    timestamp = report.scannedAtMillis,
                    inspected = report.inspectedCount,
                    matched = report.matchedCount,
                    skipped = report.skippedCount,
                )
            }
            report.copy(newlyInsertedCount = newlyInserted)
        }

    fun inspectMessageDryRun(rawMessage: String): SmsParseTrace =
        SmsTransactionParser.inspect(
            rawMessage = rawMessage,
            classifier = classifier,
            timestampMillis = System.currentTimeMillis(),
        )

    suspend fun ingestIncomingMessage(
        rawMessage: String,
        sender: String,
        timestampMillis: Long,
        source: TransactionSource,
    ): SmsParseTrace = withContext(Dispatchers.IO) {
        hydrateMlWeights()
        val trace = SmsTransactionParser.inspect(
            rawMessage = rawMessage,
            classifier = classifier,
            timestampMillis = timestampMillis,
            sender = sender,
        )
        if (trace.accepted && trace.amountInr != null && trace.merchant != null && trace.prediction != null) {
            transactionDao.insertIgnore(
                TransactionEntity(
                    amountInr = trace.amountInr,
                    merchant = trace.merchant,
                    rawSnippet = trace.sanitizedSnippet,
                    categoryName = trace.prediction.category.name,
                    timestamp = timestampMillis,
                    sourceName = source.name,
                    mlConfidence = trace.prediction.confidence,
                    isUserCorrected = false,
                    dedupeHash = trace.dedupeHash ?: SmsTransactionParser.computeDedupeHash(
                        trace.amountInr,
                        trace.merchant,
                        timestampMillis,
                    ),
                ),
            )
        }
        trace
    }

    suspend fun addManualOrQuickExpense(
        amountInr: Double,
        merchant: String,
        categoryOverride: ExpenseCategory? = null,
        note: String = "",
        source: TransactionSource = TransactionSource.MANUAL,
        timestampMillis: Long = System.currentTimeMillis(),
    ) = withContext(Dispatchers.IO) {
        val prediction = classifier.classify(merchant, note)
        val chosen = categoryOverride ?: prediction.category
        val confidence = if (categoryOverride != null && categoryOverride != prediction.category) 1.0f else prediction.confidence
        transactionDao.insertIgnore(
            TransactionEntity(
                amountInr = amountInr,
                merchant = merchant.trim(),
                rawSnippet = note.ifBlank { "Logged via ${source.badgeLabel}" },
                categoryName = chosen.name,
                timestamp = timestampMillis,
                sourceName = source.name,
                mlConfidence = confidence,
                isUserCorrected = categoryOverride != null,
                dedupeHash = SmsTransactionParser.computeDedupeHash(amountInr, merchant, timestampMillis),
            ),
        )
        if (categoryOverride != null && categoryOverride != prediction.category) {
            val updates = classifier.buildOnlineLearningUpdates(merchant, note, categoryOverride)
            mlWeightDao.upsertWeights(updates)
        }
    }

    suspend fun recategorizeTransaction(
        tx: TransactionEntity,
        newCategory: ExpenseCategory,
    ) = withContext(Dispatchers.IO) {
        transactionDao.updateCategory(tx.id, newCategory.name)
        val updates = classifier.buildOnlineLearningUpdates(tx.merchant, tx.rawSnippet, newCategory)
        mlWeightDao.upsertWeights(updates)
    }

    suspend fun deleteTransaction(id: Long) = withContext(Dispatchers.IO) {
        transactionDao.deleteById(id)
    }

    suspend fun importCsvStatement(csvLines: List<String>): Int = withContext(Dispatchers.IO) {
        hydrateMlWeights()
        val entities = mutableListOf<TransactionEntity>()
        val now = System.currentTimeMillis()
        csvLines.drop(1).forEachIndexed { index, line ->
            val cols = parseCsvLine(line)
            if (cols.size >= 2) {
                val amount = cols.firstNotNullOfOrNull { cell ->
                    cell.replace("₹", "")
                        .replace("INR", "", ignoreCase = true)
                        .replace(",", "")
                        .trim()
                        .toDoubleOrNull()
                        ?.takeIf { it > 0.0 }
                }
                val merchant = cols.firstOrNull { cell ->
                    cell.any { it.isLetter() } && !cell.startsWith("202")
                }?.trim()

                if (amount != null && !merchant.isNullOrBlank()) {
                    val prediction = classifier.classify(merchant, line)
                    val ts = now - index * 3_600_000L
                    entities.add(
                        TransactionEntity(
                            amountInr = amount,
                            merchant = merchant.take(40),
                            rawSnippet = line.take(120),
                            categoryName = prediction.category.name,
                            timestamp = ts,
                            sourceName = TransactionSource.CSV_IMPORT.name,
                            mlConfidence = prediction.confidence,
                            isUserCorrected = false,
                            dedupeHash = SmsTransactionParser.computeDedupeHash(amount, merchant, ts),
                        ),
                    )
                }
            }
        }
        if (entities.isEmpty()) return@withContext 0
        val inserted = transactionDao.insertAllIgnore(entities)
        inserted.count { it != -1L }
    }

    suspend fun clearAllTransactions() = withContext(Dispatchers.IO) {
        transactionDao.deleteAll()
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString().trim())
        return result
    }
}
