package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import com.example.ui.theme.AppIcons
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.TransactionEntity
import com.example.data.TransactionSource
import com.example.domain.ExpenseCategory
import com.example.domain.InrFormatter
import com.example.ui.CategorySummary
import com.example.ui.theme.BreezeBackground
import com.example.ui.theme.BreezeCanvas
import com.example.ui.theme.BreezeDanger
import com.example.ui.theme.BreezeInk
import com.example.ui.theme.BreezeInk2
import com.example.ui.theme.BreezeLine
import com.example.ui.theme.BreezePrimary
import com.example.ui.theme.BreezeSuccess
import com.example.ui.theme.BreezeSurface2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    category: ExpenseCategory,
    summary: CategorySummary,
    transactions: List<TransactionEntity>,
    onBack: () -> Unit,
    onRecategorize: (TransactionEntity, ExpenseCategory) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onUpdateCategoryLimit: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    var selectedSourceFilter by remember { mutableStateOf<String?>(null) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var showBudgetSheet by remember { mutableStateOf(false) }

    val filteredTransactions = remember(transactions, selectedSourceFilter) {
        if (selectedSourceFilter == null) {
            transactions
        } else {
            transactions.filter { it.sourceName == selectedSourceFilter }
        }
    }

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
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(BreezeBackground)
                                .border(1.dp, BreezeLine, CircleShape)
                                .testTag("category_detail_back")
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
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "CURATED CATEGORY",
                                style = MaterialTheme.typography.labelSmall,
                                color = BreezeInk2,
                            )
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.headlineSmall,
                                color = BreezeInk,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(BreezeBackground)
                            .border(1.dp, BreezeLine, RoundedCornerShape(50))
                            .clickable(role = Role.Button) { showBudgetSheet = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = AppIcons.Tune,
                                contentDescription = "Adjust category limit",
                                tint = BreezeInk,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Limit",
                                style = MaterialTheme.typography.labelMedium,
                                color = BreezeInk,
                            )
                        }
                    }
                }
            }

            // Hero Category Card
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(BreezeBackground)
                        .border(1.dp, BreezeLine, RoundedCornerShape(24.dp))
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(84.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val sweep = (summary.budgetProgress.coerceIn(0f, 1f) * 360f)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 8.dp.toPx()
                            drawArc(
                                color = BreezeSurface2,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round),
                            )
                            if (sweep > 1f) {
                                drawArc(
                                    color = category.ringColor,
                                    startAngle = -90f,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                                )
                            }
                        }
                        Icon(
                            imageVector = category.icon,
                            contentDescription = category.displayName,
                            tint = BreezeInk,
                            modifier = Modifier.size(26.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(18.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = InrFormatter.format(summary.spentInr),
                            style = MaterialTheme.typography.displayMedium,
                            color = BreezeInk,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val limitText = if (summary.budgetLimitInr > 0) {
                            val left = summary.budgetLimitInr - summary.spentInr
                            if (left >= 0) {
                                "${InrFormatter.format(left)} left of ${InrFormatter.format(summary.budgetLimitInr)}"
                            } else {
                                "Over limit by ${InrFormatter.format(-left)}"
                            }
                        } else {
                            "${summary.transactionCount} transactions curated"
                        }
                        Text(
                            text = limitText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (summary.budgetLimitInr > 0 && summary.spentInr > summary.budgetLimitInr) {
                                BreezeDanger
                            } else {
                                BreezeInk2
                            },
                        )

                        if (summary.topMerchants.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                summary.topMerchants.forEach { (merchant, amt) ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(BreezeSurface2)
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                    ) {
                                        Text(
                                            text = "$merchant · ${InrFormatter.formatCompact(amt)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BreezeInk,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Source Filter Chips
            item {
                val chips = listOf(
                    null to "All (${transactions.size})",
                    TransactionSource.SMS_INBOX.name to "SMS",
                    TransactionSource.SMS_LIVE.name to "Live SMS",
                    TransactionSource.SMART_PASTE.name to "Parsed",
                    TransactionSource.CSV_IMPORT.name to "CSV",
                    TransactionSource.MANUAL.name to "Manual",
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    chips.forEach { (sourceKey, label) ->
                        val selected = selectedSourceFilter == sourceKey
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) BreezePrimary else BreezeSurface2)
                                .border(
                                    width = 1.dp,
                                    color = if (selected) BreezePrimary else BreezeLine,
                                    shape = RoundedCornerShape(50),
                                )
                                .clickable(role = Role.Tab) { selectedSourceFilter = sourceKey }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) BreezeBackground else BreezeInk2,
                            )
                        }
                    }
                }
            }

            if (filteredTransactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(BreezeBackground)
                            .border(1.dp, BreezeLine, RoundedCornerShape(20.dp))
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = BreezeInk2,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No ${category.shortName.lowercase()} expenses yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = BreezeInk,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Payments from merchants in this category will appear here automatically from your SMS alerts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BreezeInk2,
                        )
                    }
                }
            } else {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(BreezeBackground)
                            .border(1.dp, BreezeLine, RoundedCornerShape(20.dp)),
                    ) {
                        filteredTransactions.forEachIndexed { idx, tx ->
                            TransactionListItem(
                                tx = tx,
                                category = category,
                                onClick = { editingTransaction = tx },
                            )
                            if (idx < filteredTransactions.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 70.dp),
                                    thickness = 1.dp,
                                    color = BreezeLine,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Tip: Tap any transaction to correct its category—FinTracker's on-device ML learns from your edit.",
                    style = MaterialTheme.typography.labelMedium,
                    color = BreezeInk2,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                )
            }
        }
    }

    // Re-categorize & ML Feedback Sheet
    editingTransaction?.let { tx ->
        ModalBottomSheet(
            onDismissRequest = { editingTransaction = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = BreezeBackground,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tx.merchant,
                            style = MaterialTheme.typography.headlineSmall,
                            color = BreezeInk,
                        )
                        Text(
                            text = "${InrFormatter.format(tx.amountInr)} · ${InrFormatter.formatTimestamp(tx.timestamp)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BreezeInk2,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(BreezeSurface2)
                            .clickable(role = Role.Button) {
                                onDeleteTransaction(tx.id)
                                editingTransaction = null
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = AppIcons.DeleteOutline,
                            contentDescription = "Delete transaction",
                            tint = BreezeDanger,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                if (tx.rawSnippet.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BreezeSurface2)
                            .padding(12.dp),
                    ) {
                        Text(
                            text = tx.rawSnippet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = BreezeInk2,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = AppIcons.AutoAwesome,
                        contentDescription = null,
                        tint = BreezeSuccess,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RE-CATEGORIZE (TRAINS ON-DEVICE ML)",
                        style = MaterialTheme.typography.labelSmall,
                        color = BreezeInk2,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExpenseCategory.entries.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            pair.forEach { target ->
                                val isCurrent = target.name == tx.categoryName
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isCurrent) BreezePrimary else BreezeSurface2)
                                        .border(
                                            1.dp,
                                            if (isCurrent) BreezePrimary else BreezeLine,
                                            RoundedCornerShape(50),
                                        )
                                        .clickable(role = Role.Button) {
                                            onRecategorize(tx, target)
                                            editingTransaction = null
                                        }
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        imageVector = target.icon,
                                        contentDescription = null,
                                        tint = if (isCurrent) BreezeBackground else BreezeInk,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = target.shortName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isCurrent) BreezeBackground else BreezeInk,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBudgetSheet) {
        var limitText by remember {
            mutableStateOf(
                if (summary.budgetLimitInr > 0) summary.budgetLimitInr.toLong().toString() else "5000",
            )
        }
        ModalBottomSheet(
            onDismissRequest = { showBudgetSheet = false },
            containerColor = BreezeBackground,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
            ) {
                Text(
                    text = "${category.displayName} Limit",
                    style = MaterialTheme.typography.headlineSmall,
                    color = BreezeInk,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Adjust your monthly target for this category in INR (₹).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BreezeInk2,
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { input -> limitText = input.filter { it.isDigit() } },
                    label = { Text("Category Limit (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(BreezePrimary)
                        .clickable(role = Role.Button) {
                            limitText.toDoubleOrNull()?.takeIf { it > 0 }?.let {
                                onUpdateCategoryLimit(it)
                                showBudgetSheet = false
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Save Category Limit",
                        style = MaterialTheme.typography.labelLarge,
                        color = BreezeBackground,
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionListItem(
    tx: TransactionEntity,
    category: ExpenseCategory,
    onClick: () -> Unit,
) {
    val sourceLabel = TransactionSource.entries
        .firstOrNull { it.name == tx.sourceName }
        ?.badgeLabel ?: "Auto"

    val confidenceLabel = if (tx.isUserCorrected) {
        "Learned"
    } else {
        "${(tx.mlConfidence * 100).toInt()}% ML"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
                imageVector = category.icon,
                contentDescription = category.displayName,
                tint = BreezeInk,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.merchant,
                style = MaterialTheme.typography.titleMedium,
                color = BreezeInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${InrFormatter.formatTimestamp(tx.timestamp)} · $sourceLabel · $confidenceLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = BreezeInk2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = InrFormatter.format(tx.amountInr, showDecimals = tx.amountInr % 1.0 != 0.0),
            style = MaterialTheme.typography.titleMedium,
            color = BreezeInk,
        )
    }
}
