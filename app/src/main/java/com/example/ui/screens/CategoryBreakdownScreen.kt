package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.ExpenseCategory
import com.example.domain.InrFormatter
import com.example.ui.CategorySummary
import com.example.ui.FinTrackerUiState
import com.example.ui.components.CategoryDistributionReport
import com.example.ui.components.DailySpendPaceChart
import com.example.ui.theme.BreezeBackground
import com.example.ui.theme.BreezeCanvas
import com.example.ui.theme.BreezeDanger
import com.example.ui.theme.BreezeInk
import com.example.ui.theme.BreezeInk2
import com.example.ui.theme.BreezeLine
import com.example.ui.theme.BreezePrimary
import com.example.ui.theme.BreezeSuccess
import com.example.ui.theme.BreezeSurface2
import com.example.ui.theme.BreezeWarning

@Composable
fun CategoryBreakdownScreen(
    uiState: FinTrackerUiState,
    onBack: () -> Unit,
    onCategoryClick: (ExpenseCategory) -> Unit,
    onOpenGoalEditor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    val targetDailyPace = if (uiState.hasMonthlyGoal) {
        uiState.monthlyGoalInr / 30.0
    } else {
        0.0
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(BreezeBackground)
                            .border(1.dp, BreezeLine, CircleShape)
                            .testTag("breakdown_back_button")
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.monthTitle.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = BreezeInk2,
                        )
                        Text(
                            text = "Categorical Spending",
                            style = MaterialTheme.typography.headlineLarge,
                            color = BreezeInk,
                        )
                    }
                }
            }

            // Monthly Summary Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(BreezeBackground)
                        .border(1.dp, BreezeLine, RoundedCornerShape(22.dp))
                        .padding(20.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Column {
                            Text(
                                text = "TOTAL SPENT",
                                style = MaterialTheme.typography.labelSmall,
                                color = BreezeInk2,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = InrFormatter.format(uiState.totalSpentInr),
                                style = MaterialTheme.typography.displayMedium,
                                color = BreezeInk,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(BreezeSurface2)
                                .border(1.dp, BreezeLine, RoundedCornerShape(50))
                                .clickable(role = Role.Button, onClick = onOpenGoalEditor)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = if (uiState.hasMonthlyGoal) {
                                    "Goal ${InrFormatter.formatCompact(uiState.monthlyGoalInr)}"
                                } else {
                                    "Set Goal"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = BreezeInk,
                            )
                        }
                    }

                    if (uiState.hasMonthlyGoal) {
                        Spacer(modifier = Modifier.height(14.dp))
                        val progressColor = when {
                            uiState.overallProgress > 1f -> BreezeDanger
                            uiState.overallProgress >= 0.8f -> BreezeWarning
                            else -> BreezePrimary
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(BreezeSurface2),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(uiState.overallProgress.coerceIn(0.02f, 1f))
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(progressColor),
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = if (uiState.remainingInr >= 0) {
                                    "${InrFormatter.format(uiState.remainingInr)} remaining"
                                } else {
                                    "Over goal by ${InrFormatter.format(-uiState.remainingInr)}"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (uiState.remainingInr >= 0) BreezeSuccess else BreezeDanger,
                            )
                            Text(
                                text = "Safe ${InrFormatter.format(uiState.safeDailySpendInr)}/day",
                                style = MaterialTheme.typography.labelMedium,
                                color = BreezeInk2,
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "TAP A CATEGORY TO EXPLORE",
                    style = MaterialTheme.typography.labelSmall,
                    color = BreezeInk2,
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                )
            }

            items(
                items = uiState.categorySummaries,
                key = { it.category.name },
            ) { summary ->
                CategorySummaryCard(
                    summary = summary,
                    onClick = { onCategoryClick(summary.category) },
                )
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "VISUAL REPORTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = BreezeInk2,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            item {
                DailySpendPaceChart(
                    dailySeries = uiState.dailySeries,
                    targetDailyPaceInr = targetDailyPace,
                )
            }

            item {
                CategoryDistributionReport(
                    summaries = uiState.categorySummaries,
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun CategorySummaryCard(
    summary: CategorySummary,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BreezeBackground)
            .border(1.dp, BreezeLine, RoundedCornerShape(20.dp))
            .testTag("category_card_${summary.category.shortName.lowercase()}")
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Mini Ring + Category Icon
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            val sweep = (summary.budgetProgress.coerceIn(0f, 1f) * 360f)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 4.dp.toPx()
                drawArc(
                    color = BreezeSurface2,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                if (sweep > 1f) {
                    drawArc(
                        color = summary.category.ringColor,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(BreezeSurface2),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = summary.category.icon,
                    contentDescription = summary.category.displayName,
                    tint = BreezeInk,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = summary.category.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = BreezeInk,
            )
            Spacer(modifier = Modifier.height(2.dp))
            val meta = if (summary.transactionCount == 0) {
                "No transactions yet"
            } else {
                val txWord = if (summary.transactionCount == 1) "expense" else "expenses"
                val sharePct = (summary.shareOfTotal * 100).toInt()
                "${summary.transactionCount} $txWord · $sharePct% of total"
            }
            Text(
                text = meta,
                style = MaterialTheme.typography.bodyMedium,
                color = BreezeInk2,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = InrFormatter.format(summary.spentInr),
                style = MaterialTheme.typography.titleMedium,
                color = BreezeInk,
            )
            if (summary.budgetLimitInr > 0) {
                Text(
                    text = "of ${InrFormatter.formatCompact(summary.budgetLimitInr)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (summary.spentInr > summary.budgetLimitInr) BreezeDanger else BreezeInk2,
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = BreezeInk2,
            modifier = Modifier.size(20.dp),
        )
    }
}
