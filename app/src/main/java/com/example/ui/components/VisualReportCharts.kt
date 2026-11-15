package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import com.example.domain.InrFormatter
import com.example.ui.CategorySummary
import com.example.ui.DailySpendPoint
import com.example.ui.theme.BreezeBackground
import com.example.ui.theme.BreezeInk
import com.example.ui.theme.BreezeInk2
import com.example.ui.theme.BreezeLine
import com.example.ui.theme.BreezePrimary
import com.example.ui.theme.BreezeSurface2
import com.example.ui.theme.BreezeTertiary
import com.example.ui.theme.BreezeWarning

@Composable
fun DailySpendPaceChart(
    dailySeries: List<DailySpendPoint>,
    targetDailyPaceInr: Double,
    modifier: Modifier = Modifier,
) {
    var mounted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { mounted = true }
    val animProgress by animateFloatAsState(
        targetValue = if (mounted) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "barChartReveal",
    )

    val maxBar = maxOf(
        dailySeries.maxOfOrNull { it.amountInr } ?: 0.0,
        targetDailyPaceInr * 1.3,
        100.0,
    )

    Column(
        modifier = modifier
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
            Column {
                Text(
                    text = "DAILY SPENDING PACE",
                    style = MaterialTheme.typography.labelSmall,
                    color = BreezeInk2,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (targetDailyPaceInr > 0) {
                        "Safe limit ${InrFormatter.format(targetDailyPaceInr)}/day"
                    } else {
                        "Last 14 days"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = BreezeInk,
                )
            }
            if (dailySeries.isNotEmpty()) {
                val peak = dailySeries.maxOf { it.amountInr }
                Text(
                    text = "Peak ${InrFormatter.formatCompact(peak)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = BreezeInk2,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (dailySeries.isEmpty() || dailySeries.all { it.amountInr == 0.0 }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BreezeSurface2),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Daily bars appear once expenses are captured",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BreezeInk2,
                )
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp),
            ) {
                val count = dailySeries.size.coerceAtLeast(1)
                val slotWidth = size.width / count
                val barWidth = (slotWidth * 0.54f).coerceIn(6.dp.toPx(), 18.dp.toPx())
                val chartHeight = size.height - 6.dp.toPx()

                // Dashed reference line for daily safe pace
                if (targetDailyPaceInr > 0) {
                    val ratio = (targetDailyPaceInr / maxBar).toFloat().coerceIn(0f, 1f)
                    val lineY = chartHeight - (ratio * chartHeight)
                    drawLine(
                        color = BreezeTertiary,
                        start = Offset(0f, lineY),
                        end = Offset(size.width, lineY),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
                    )
                }

                dailySeries.forEachIndexed { idx, point ->
                    val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                    val frac = (point.amountInr / maxBar).toFloat().coerceIn(0f, 1f) * animProgress
                    val barH = (frac * chartHeight).coerceAtLeast(if (point.amountInr > 0) 4.dp.toPx() else 2.dp.toPx())
                    val y = chartHeight - barH

                    val barColor = when {
                        point.amountInr == 0.0 -> BreezeSurface2
                        point.isOverPace -> BreezeWarning
                        else -> BreezePrimary
                    }

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                dailySeries.forEachIndexed { idx, pt ->
                    if (idx == 0 || idx == dailySeries.lastIndex || idx == dailySeries.size / 2) {
                        Text(
                            text = "Day ${pt.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BreezeInk2,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryDistributionReport(
    summaries: List<CategorySummary>,
    modifier: Modifier = Modifier,
) {
    val active = summaries.filter { it.spentInr > 0.0 }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BreezeBackground)
            .border(1.dp, BreezeLine, RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Text(
            text = "CATEGORY DISTRIBUTION",
            style = MaterialTheme.typography.labelSmall,
            color = BreezeInk2,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (active.isEmpty()) "No spend recorded yet" else "${active.size} active spending categories",
            style = MaterialTheme.typography.titleMedium,
            color = BreezeInk,
        )
        Spacer(modifier = Modifier.height(14.dp))

        if (active.isNotEmpty()) {
            // Segmented proportional pill track
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(BreezeSurface2),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                active.forEach { item ->
                    Box(
                        modifier = Modifier
                            .weight(item.shareOfTotal.coerceAtLeast(0.03f))
                            .height(12.dp)
                            .background(item.category.ringColor),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                active.take(5).forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(item.category.ringColor),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.category.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = BreezeInk,
                            )
                        }
                        Text(
                            text = "${(item.shareOfTotal * 100).toInt()}% · ${InrFormatter.format(item.spentInr)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = BreezeInk,
                        )
                    }
                }
            }
        }
    }
}
