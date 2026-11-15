package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.example.R
import com.example.domain.InrFormatter
import com.example.ui.CategorySummary
import com.example.ui.theme.BreezeDanger
import com.example.ui.theme.BreezeInk
import com.example.ui.theme.BreezeInk2
import com.example.ui.theme.BreezeLine
import com.example.ui.theme.BreezePrimary
import com.example.ui.theme.BreezeSuccess
import com.example.ui.theme.BreezeSurface2
import com.example.ui.theme.BreezeTertiary
import com.example.ui.theme.BreezeWarning

@Composable
fun ConcentricSpendingCircle(
    totalSpentInr: Double,
    monthlyGoalInr: Double,
    hasMonthlyGoal: Boolean,
    overallProgress: Float,
    categorySummaries: List<CategorySummary>,
    hasSmsPermission: Boolean,
    onCircleClick: () -> Unit,
    onSubtleAddClick: () -> Unit,
    onSetGoalClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var mounted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { mounted = true }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "circlePressScale",
    )

    val activeCategories = remember(categorySummaries) {
        categorySummaries.filter { it.spentInr > 0.0 }.take(4)
    }

    val targetOuterSweep = if (mounted) (overallProgress.coerceIn(0f, 1f) * 360f) else 0f
    val animatedOuterSweep by animateFloatAsState(
        targetValue = targetOuterSweep,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "outerSweep",
    )

    val ringColors = listOf(
        when {
            overallProgress > 1.0f -> BreezeDanger
            overallProgress >= 0.80f -> BreezeWarning
            else -> BreezePrimary
        },
    ) + activeCategories.map { it.category.ringColor }

    val targetSweeps = listOf(animatedOuterSweep) + activeCategories.mapIndexed { idx, summary ->
        val raw = (summary.budgetProgress.coerceAtLeast(summary.shareOfTotal)).coerceIn(0.04f, 1f)
        val animated by animateFloatAsState(
            targetValue = if (mounted) raw * 360f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessVeryLow,
            ),
            label = "innerSweep_$idx",
        )
        animated
    }

    // Ensure at least 4 concentric tracks are always visible (empty tracks in #F3F5F7 when empty)
    val trackCount = maxOf(4, targetSweeps.size)

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val diameter = min(maxWidth, maxHeight).coerceIn(260.dp, 368.dp)

        Box(
            modifier = Modifier
                .size(diameter)
                .scale(pressScale)
                .clip(CircleShape)
                .testTag("concentric_spending_circle")
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true),
                    role = Role.Button,
                    onClick = onCircleClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = 10.dp.toPx()
                val gapPx = 4.dp.toPx()
                val stepPx = strokePx + gapPx
                val halfSize = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                for (index in 0 until trackCount) {
                    val radius = halfSize - (strokePx / 2f) - (index * stepPx) - 2.dp.toPx()
                    if (radius <= 0f) break

                    val topLeft = Offset(center.x - radius, center.y - radius)
                    val arcSize = Size(radius * 2f, radius * 2f)

                    // Recessed track ring
                    drawArc(
                        color = BreezeSurface2,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                    )

                    // Active telemetry ring
                    val sweep = targetSweeps.getOrNull(index) ?: 0f
                    val color = ringColors.getOrNull(index) ?: Color.Transparent
                    if (sweep > 0.5f) {
                        drawArc(
                            color = color,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round),
                        )
                    }
                }
            }

            // Center Hub Readout + Subtle '+' Affordance
            Column(
                modifier = Modifier
                    .padding(horizontal = 44.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (!hasMonthlyGoal && totalSpentInr == 0.0) {
                    Text(
                        text = "READY TO TRACK",
                        style = MaterialTheme.typography.labelSmall,
                        color = BreezeInk2,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹0",
                        style = MaterialTheme.typography.displayMedium,
                        color = BreezeInk,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap to set monthly goal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BreezeInk2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onSetGoalClick)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                } else {
                    Text(
                        text = "SPENT THIS MONTH",
                        style = MaterialTheme.typography.labelSmall,
                        color = BreezeInk2,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = InrFormatter.format(totalSpentInr),
                        style = MaterialTheme.typography.displayMedium,
                        color = BreezeInk,
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val subLabel = when {
                        hasMonthlyGoal && totalSpentInr > monthlyGoalInr ->
                            "Over by ${InrFormatter.format(totalSpentInr - monthlyGoalInr)}"
                        hasMonthlyGoal ->
                            "${InrFormatter.format(monthlyGoalInr - totalSpentInr)} left of ${InrFormatter.formatCompact(monthlyGoalInr)}"
                        else ->
                            if (hasSmsPermission) "Auto-tracking SMS" else "Tap ring to view"
                    }
                    val subColor = when {
                        hasMonthlyGoal && totalSpentInr > monthlyGoalInr -> BreezeDanger
                        hasMonthlyGoal && overallProgress >= 0.8f -> BreezeWarning
                        hasMonthlyGoal && totalSpentInr > 0.0 -> BreezeSuccess
                        else -> BreezeInk2
                    }
                    Text(
                        text = subLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = subColor,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Subtle '+' button inside hub for Smart Paste / SMS Scan / Quick Log
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(BreezeSurface2)
                        .border(1.dp, BreezeLine, CircleShape)
                        .testTag("subtle_add_button")
                        .clickable(
                            role = Role.Button,
                            onClick = onSubtleAddClick,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.add_expense_cd),
                        tint = BreezeInk,
                        modifier = Modifier.size(20.dp),
                    )
                }

                if (totalSpentInr > 0.0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap ring for categories",
                        style = MaterialTheme.typography.labelSmall,
                        color = BreezeTertiary,
                    )
                }
            }
        }
    }
}
