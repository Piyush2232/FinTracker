package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.capture.SmsTransactionParser
import com.example.domain.ExpenseCategory
import com.example.domain.InrFormatter
import com.example.ml.MlPrediction
import com.example.ui.FinTrackerUiState
import com.example.ui.theme.BreezeBackground
import com.example.ui.theme.BreezeCanvas
import com.example.ui.theme.BreezeDanger
import com.example.ui.theme.BreezeInk
import com.example.ui.theme.BreezeInk2
import com.example.ui.theme.BreezeLine
import com.example.ui.theme.BreezePrimary
import com.example.ui.theme.BreezeSuccess
import com.example.ui.theme.BreezeSuccessSoft
import com.example.ui.theme.BreezeSurface2
import com.example.ui.theme.BreezeWarning
import com.example.ui.theme.BreezeWarningSoft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyGoalOnboardingSheet(
    monthTitle: String,
    suggestedGoalInr: Double,
    hasSmsPermission: Boolean,
    onSaveGoal: (Double) -> Unit,
    onRequestSmsPermission: () -> Unit,
    onOpenParserLab: () -> Unit,
    onDismiss: () -> Unit,
) {
    var goalInput by remember(suggestedGoalInr) {
        mutableStateOf(suggestedGoalInr.toLong().toString())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = BreezeBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = "$monthTitle · SETUP",
                style = MaterialTheme.typography.labelSmall,
                color = BreezeInk2,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Set your monthly goal",
                style = MaterialTheme.typography.headlineLarge,
                color = BreezeInk,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "FinTracker builds your concentric budget rings around this target. All data stays strictly on your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = BreezeInk2,
            )

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = goalInput,
                onValueChange = { raw -> goalInput = raw.filter { it.isDigit() } },
                label = { Text("Monthly Spending Goal (₹)") },
                prefix = { Text("₹ ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("monthly_goal_input"),
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(25_000L, 50_000L, 75_000L, 1_00_000L).forEach { preset ->
                    val selected = goalInput == preset.toString()
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) BreezePrimary else BreezeSurface2)
                            .border(1.dp, if (selected) BreezePrimary else BreezeLine, RoundedCornerShape(50))
                            .clickable(role = Role.Button) { goalInput = preset.toString() }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = InrFormatter.format(preset.toDouble()),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) BreezeBackground else BreezeInk,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            // Privacy-first SMS permission card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BreezeSurface2)
                    .border(1.dp, BreezeLine, RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Sms,
                        contentDescription = null,
                        tint = BreezeInk,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasSmsPermission) {
                            "Local SMS Auto-Tracking Enabled"
                        } else {
                            "Automatic Expense Parsing via SMS"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = BreezeInk,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Reads bank & UPI debit SMS strictly on-device, ignoring OTPs and promos. Zero bytes leave your phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BreezeInk2,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!hasSmsPermission) {
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(50))
                                .background(BreezeBackground)
                                .border(1.dp, BreezeLine, RoundedCornerShape(50))
                                .testTag("grant_sms_button")
                                .clickable(role = Role.Button, onClick = onRequestSmsPermission)
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Allow SMS Scan",
                                style = MaterialTheme.typography.labelMedium,
                                color = BreezeInk,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(50))
                            .background(BreezeBackground)
                            .border(1.dp, BreezeLine, RoundedCornerShape(50))
                            .clickable(role = Role.Button, onClick = onOpenParserLab)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Test Parser Lab",
                            style = MaterialTheme.typography.labelMedium,
                            color = BreezeInk,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(50))
                    .background(BreezePrimary)
                    .testTag("confirm_monthly_goal_button")
                    .clickable(role = Role.Button) {
                        val goal = goalInput.toDoubleOrNull() ?: suggestedGoalInr
                        if (goal > 0) onSaveGoal(goal)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Confirm Monthly Goal",
                    style = MaterialTheme.typography.labelLarge,
                    color = BreezeBackground,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakModalSheet(
    uiState: FinTrackerUiState,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BreezeBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(BreezeWarningSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = BreezeWarning,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "${uiState.streakCount}-Day Budget Streak",
                        style = MaterialTheme.typography.headlineSmall,
                        color = BreezeInk,
                    )
                    Text(
                        text = if (uiState.hasMonthlyGoal) {
                            "Safe daily target: ${InrFormatter.format(uiState.safeDailySpendInr)}/day"
                        } else {
                            "Set a monthly goal to track daily budget pace"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = BreezeInk2,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(BreezeSurface2)
                    .border(1.dp, BreezeLine, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                uiState.streakDots.forEach { dot ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        dot.onPace && (dot.hasActivity || uiState.hasMonthlyGoal) -> BreezeSuccess
                                        dot.hasActivity && !dot.onPace -> BreezeWarning
                                        else -> BreezeBackground
                                    },
                                )
                                .border(1.dp, BreezeLine, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = dot.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (dot.onPace && (dot.hasActivity || uiState.hasMonthlyGoal)) {
                                    BreezeBackground
                                } else {
                                    BreezeInk2
                                },
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (dot.spentInr > 0) InrFormatter.formatCompact(dot.spentInr) else "—",
                            style = MaterialTheme.typography.labelSmall,
                            color = BreezeInk2,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtleAddCaptureSheet(
    hasSmsPermission: Boolean,
    onPreviewMl: (String, String) -> MlPrediction,
    onQuickLog: (Double, String, ExpenseCategory?, String) -> Unit,
    onParseAndSaveMessage: (String) -> Unit,
    onScanSmsNow: () -> Unit,
    onImportCsv: () -> Unit,
    onOpenParserLab: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Smart Paste SMS, 1 = Quick Log
    var smsText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var merchantText by remember { mutableStateOf("") }
    var selectedCategoryOverride by remember { mutableStateOf<ExpenseCategory?>(null) }

    val livePrediction = remember(merchantText) {
        if (merchantText.isNotBlank()) onPreviewMl(merchantText, "") else null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = BreezeBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = "CAPTURE EXPENSE",
                style = MaterialTheme.typography.labelSmall,
                color = BreezeInk2,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Automatic or quick entry",
                style = MaterialTheme.typography.headlineSmall,
                color = BreezeInk,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Mode Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(BreezeSurface2)
                    .border(1.dp, BreezeLine, RoundedCornerShape(50))
                    .padding(4.dp),
            ) {
                listOf("Paste SMS / Alert", "Quick Log").forEachIndexed { idx, label ->
                    val active = selectedTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (active) BreezePrimary else BreezeSurface2)
                            .clickable(role = Role.Tab) { selectedTab = idx },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (active) BreezeBackground else BreezeInk2,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedTab == 0) {
                OutlinedTextField(
                    value = smsText,
                    onValueChange = { smsText = it },
                    label = { Text("Paste bank / UPI SMS message") },
                    placeholder = { Text("e.g. Amt Sent Rs.485.00 To SWIGGY...") },
                    minLines = 3,
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(BreezePrimary)
                        .clickable(role = Role.Button) {
                            if (smsText.isNotBlank()) {
                                onParseAndSaveMessage(smsText)
                                onDismiss()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Parse & Categorize Locally",
                        style = MaterialTheme.typography.labelLarge,
                        color = BreezeBackground,
                    )
                }
            } else {
                OutlinedTextField(
                    value = merchantText,
                    onValueChange = { merchantText = it },
                    label = { Text("Merchant or VPA (e.g. Swiggy, Patel Bros)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { raw ->
                        amountText = raw.filter { it.isDigit() || it == '.' }
                    },
                    label = { Text("Amount (₹)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                val activeCat = selectedCategoryOverride ?: livePrediction?.category
                if (activeCat != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (selectedCategoryOverride == null && livePrediction != null) {
                            "ML Predicted: ${activeCat.displayName} (${(livePrediction.confidence * 100).toInt()}%)"
                        } else {
                            "Category: ${activeCat.displayName}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = BreezeSuccess,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ExpenseCategory.entries.forEach { cat ->
                        val chosen = (selectedCategoryOverride ?: livePrediction?.category) == cat
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (chosen) BreezePrimary else BreezeSurface2)
                                .border(1.dp, if (chosen) BreezePrimary else BreezeLine, RoundedCornerShape(50))
                                .clickable(role = Role.Button) { selectedCategoryOverride = cat }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = cat.shortName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (chosen) BreezeBackground else BreezeInk2,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(BreezePrimary)
                        .clickable(role = Role.Button) {
                            val amt = amountText.toDoubleOrNull()
                            if (amt != null && amt > 0 && merchantText.isNotBlank()) {
                                onQuickLog(amt, merchantText, selectedCategoryOverride, "")
                                onDismiss()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Log Expense",
                        style = MaterialTheme.typography.labelLarge,
                        color = BreezeBackground,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(thickness = 1.dp, color = BreezeLine)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(BreezeSurface2)
                        .border(1.dp, BreezeLine, RoundedCornerShape(50))
                        .clickable(role = Role.Button) {
                            onDismiss()
                            onScanSmsNow()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (hasSmsPermission) "Scan SMS" else "Allow SMS",
                        style = MaterialTheme.typography.labelMedium,
                        color = BreezeInk,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(BreezeSurface2)
                        .border(1.dp, BreezeLine, RoundedCornerShape(50))
                        .clickable(role = Role.Button) {
                            onDismiss()
                            onImportCsv()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Import CSV",
                        style = MaterialTheme.typography.labelMedium,
                        color = BreezeInk,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(BreezeSurface2)
                        .border(1.dp, BreezeLine, RoundedCornerShape(50))
                        .clickable(role = Role.Button) {
                            onDismiss()
                            onOpenParserLab()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Parser Lab",
                        style = MaterialTheme.typography.labelMedium,
                        color = BreezeInk,
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileAndParserLabScreen(
    uiState: FinTrackerUiState,
    onBack: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onScanSmsInboxNow: () -> Unit,
    onSimulateSmsBatch: (Int) -> Unit = {},
    onOpenNotificationListenerSettings: () -> Unit,
    onParserLabInputChange: (String) -> Unit,
    onSaveParserLabToLedger: () -> Unit,
    onImportCsv: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onEditMonthlyGoal: () -> Unit,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BreezeCanvas)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(BreezeBackground)
                            .border(1.dp, BreezeLine, CircleShape)
                            .testTag("profile_back_button")
                            .clickable(role = Role.Button, onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back_cd),
                            tint = BreezeInk,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "PROFILE & PRIVACY HUB",
                            style = MaterialTheme.typography.labelSmall,
                            color = BreezeInk2,
                        )
                        Text(
                            text = "FinTracker Local",
                            style = MaterialTheme.typography.headlineLarge,
                            color = BreezeInk,
                        )
                    }
                }
            }

            // Local Privacy Shield Card
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(BreezeSuccessSoft)
                        .border(1.dp, BreezeLine, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = BreezeSuccess,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "100% On-Device Privacy",
                            style = MaterialTheme.typography.titleMedium,
                            color = BreezeInk,
                        )
                        Text(
                            text = "ContentObserver service, Indian regex parser, ML classifier, and Room storage run offline with zero cloud uploads.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BreezeInk2,
                        )
                    }
                }
            }

            // SmsContentObserverService + Inbox Scan Diagnostics Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(BreezeBackground)
                        .border(1.dp, BreezeLine, RoundedCornerShape(20.dp))
                        .padding(20.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Sms,
                                contentDescription = null,
                                tint = BreezeInk,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ContentObserver SMS Service",
                                style = MaterialTheme.typography.titleMedium,
                                color = BreezeInk,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (uiState.smsObserverStatus.isServiceRunning) BreezeSuccessSoft else BreezeSurface2,
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = if (uiState.smsObserverStatus.isServiceRunning) "ACTIVE" else "STANDBY",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.smsObserverStatus.isServiceRunning) BreezeSuccess else BreezeInk2,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Watching ${uiState.smsObserverStatus.observedUri} · ${uiState.smsObserverStatus.changeEventsCount} observer events",
                        style = MaterialTheme.typography.labelMedium,
                        color = BreezeInk,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = uiState.smsObserverStatus.lastDetectedSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = BreezeInk2,
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    val scanSummary = when {
                        !uiState.hasSmsPermission -> "SMS permission not granted yet (or use the Emulator SMS Simulator below)."
                        uiState.prefs.lastSmsScanTimestamp > 0L ->
                            "Last inbox scan: ${uiState.prefs.lastScanInspected} SMS inspected · ${uiState.prefs.lastScanMatched} matched · ${uiState.prefs.lastScanSkipped} OTP/promo skipped"
                        else -> "SMS permission active. Ready to scan ${uiState.monthTitle} inbox."
                    }
                    Text(
                        text = scanSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = BreezeInk2,
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(50))
                                .background(BreezePrimary)
                                .testTag("scan_sms_button")
                                .clickable(role = Role.Button) {
                                    if (uiState.hasSmsPermission) onScanSmsInboxNow() else onRequestSmsPermission()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = when {
                                    uiState.isScanningSms -> "Scanning..."
                                    uiState.hasSmsPermission -> "Scan Inbox Now"
                                    else -> "Allow SMS Access"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = BreezeBackground,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(50))
                                .background(BreezeSurface2)
                                .border(1.dp, BreezeLine, RoundedCornerShape(50))
                                .clickable(role = Role.Button, onClick = onOpenNotificationListenerSettings),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (uiState.isNotificationListenerEnabled) BreezeSuccess else BreezeInk,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.isNotificationListenerEnabled) "Alerts On" else "UPI Alerts",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BreezeInk,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(thickness = 1.dp, color = BreezeLine)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "EMULATOR LIVE SMS SIMULATOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = BreezeInk2,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Send realistic Indian bank & UPI SMS alerts directly into SmsContentObserverService:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BreezeInk2,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(1 to "+1 Bank SMS", 3 to "+3 Bank SMS", 5 to "+5 Bank SMS").forEach { (count, label) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(BreezeSurface2)
                                    .border(1.dp, BreezeLine, RoundedCornerShape(50))
                                    .testTag("simulate_sms_${count}_button")
                                    .clickable(role = Role.Button) { onSimulateSmsBatch(count) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BreezeInk,
                                )
                            }
                        }
                    }
                }
            }

            // Interactive SMS Parser Lab (Dry-Run Tester + Presets)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(BreezeBackground)
                        .border(1.dp, BreezeLine, RoundedCornerShape(20.dp))
                        .padding(20.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Science,
                            contentDescription = null,
                            tint = BreezeInk,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Indian Regex SMS Parser Lab",
                            style = MaterialTheme.typography.titleMedium,
                            color = BreezeInk,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Test Indian financial regex patterns ('Debited', 'Spent', 'INR', 'Rs.', '₹') and OTP/promo filters live.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BreezeInk2,
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "SAMPLE MESSAGES",
                        style = MaterialTheme.typography.labelSmall,
                        color = BreezeInk2,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SmsTransactionParser.testPresets.forEach { preset ->
                            val selected = uiState.parserLabInput == preset.rawMessage
                            Box(
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (selected) BreezePrimary else BreezeSurface2)
                                    .border(1.dp, if (selected) BreezePrimary else BreezeLine, RoundedCornerShape(50))
                                    .clickable(role = Role.Button) {
                                        onParserLabInputChange(preset.rawMessage)
                                    }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) BreezeBackground else BreezeInk,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.parserLabInput,
                        onValueChange = onParserLabInputChange,
                        label = { Text("Test SMS Message") },
                        minLines = 3,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("parser_lab_input"),
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    uiState.parserLabTrace?.let { trace ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(BreezeSurface2)
                                .border(1.dp, BreezeLine, RoundedCornerShape(16.dp))
                                .padding(14.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = trace.statusTitle.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (trace.accepted) BreezeSuccess else BreezeWarning,
                                )
                                Text(
                                    text = listOfNotNull(
                                        trace.matchedRegexRule,
                                        trace.instrument,
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BreezeInk2,
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            if (trace.accepted) {
                                val pred = trace.prediction
                                Text(
                                    text = "${trace.merchant} · ${InrFormatter.format(trace.amountInr ?: 0.0, showDecimals = true)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BreezeInk,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Regex Match: verb='${trace.matchedDebitKeyword ?: "debit"}' · rule=${trace.matchedRegexRule ?: "STANDARD"}${trace.accountTail?.let { " · A/c $it" } ?: ""}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BreezeInk2,
                                )
                                if (pred != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "ML Category: ${pred.category.displayName} (${(pred.confidence * 100).toInt()}% confidence)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BreezeInk,
                                    )
                                    if (pred.topTokens.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Matched tokens: ${pred.topTokens.joinToString(", ")}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = BreezeInk2,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(BreezePrimary)
                                        .testTag("parser_lab_save_button")
                                        .clickable(role = Role.Button, onClick = onSaveParserLabToLedger),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            tint = BreezeBackground,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Save Parsed Expense to Ledger",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = BreezeBackground,
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = trace.rejectionReason ?: "Message did not match an expense",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BreezeInk,
                                )
                            }
                        }
                    }
                }
            }

            // Monthly Goal & CSV Import + Exports
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(BreezeBackground)
                        .border(1.dp, BreezeLine, RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "BUDGET & STATEMENTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = BreezeInk2,
                    )

                    ActionRowItem(
                        icon = Icons.Outlined.Description,
                        title = "Monthly Budget Goal",
                        subtitle = if (uiState.hasMonthlyGoal) {
                            "${uiState.monthTitle}: ${InrFormatter.format(uiState.monthlyGoalInr)}"
                        } else {
                            "Not configured yet"
                        },
                        onClick = onEditMonthlyGoal,
                    )

                    HorizontalDivider(thickness = 1.dp, color = BreezeLine)

                    ActionRowItem(
                        icon = Icons.Outlined.FileUpload,
                        title = "Import Bank / UPI Statement (CSV)",
                        subtitle = "Parse & auto-categorize CSV transactions locally",
                        onClick = onImportCsv,
                    )

                    HorizontalDivider(thickness = 1.dp, color = BreezeLine)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(50))
                                .background(BreezeSurface2)
                                .border(1.dp, BreezeLine, RoundedCornerShape(50))
                                .testTag("export_csv_button")
                                .clickable(role = Role.Button, onClick = onExportCsv),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.TableChart,
                                    contentDescription = null,
                                    tint = BreezeInk,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Export CSV",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = BreezeInk,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(50))
                                .background(BreezePrimary)
                                .testTag("export_pdf_button")
                                .clickable(role = Role.Button, onClick = onExportPdf),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.PictureAsPdf,
                                    contentDescription = null,
                                    tint = BreezeBackground,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Export PDF",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = BreezeBackground,
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.transactions.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(50))
                            .background(BreezeBackground)
                            .border(1.dp, BreezeLine, RoundedCornerShape(50))
                            .clickable(role = Role.Button, onClick = onClearAllData),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Clear Local Transactions (${uiState.transactions.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = BreezeDanger,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ActionRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BreezeSurface2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BreezeInk,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = BreezeInk,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = BreezeInk2,
            )
        }
    }
}
