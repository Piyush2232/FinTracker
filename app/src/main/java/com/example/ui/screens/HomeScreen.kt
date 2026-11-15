package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Person
import com.example.ui.theme.AppIcons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.TransactionEntity
import com.example.data.TransactionSource
import com.example.domain.ExpenseCategory
import com.example.domain.InrFormatter
import com.example.ui.FinTrackerUiState
import com.example.ui.components.ConcentricSpendingCircle
import com.example.ui.theme.BreezeBackground
import com.example.ui.theme.BreezeInk
import com.example.ui.theme.BreezeInk2
import com.example.ui.theme.BreezeInverse
import com.example.ui.theme.BreezeLine
import com.example.ui.theme.BreezePrimary
import com.example.ui.theme.BreezeSuccess
import com.example.ui.theme.BreezeSurface2
import com.example.ui.theme.BreezeWarning
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    uiState: FinTrackerUiState,
    onCircleClick: () -> Unit,
    onSubtleAddClick: () -> Unit,
    onSetGoalClick: () -> Unit,
    onStreakClick: () -> Unit,
    onProfileClick: () -> Unit,
    onCategoryClick: (ExpenseCategory) -> Unit = {},
    onSimulateBankSms: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Compute smooth scroll progress (0f at top -> 1f when scrolled into Recent Spending)
    val scrollFraction by remember {
        derivedStateOf {
            when {
                listState.firstVisibleItemIndex > 0 -> 1f
                else -> (listState.firstVisibleItemScrollOffset / 520f).coerceIn(0f, 1f)
            }
        }
    }

    val animatedScrollFraction by animateFloatAsState(
        targetValue = scrollFraction,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "homeScrollFraction",
    )

    val recentSectionVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 70
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scrollCueBounce")
    val cueOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cueBounceY",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(BreezeBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        val viewportHeight = maxHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter),
        ) {
            // Minimal Header: ONLY Streak and Profile buttons aligned top-right (Zero footer)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(BreezeSurface2)
                        .border(1.dp, BreezeLine, RoundedCornerShape(50))
                        .testTag("streak_button")
                        .clickable(role = Role.Button, onClick = onStreakClick)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = AppIcons.LocalFireDepartment,
                        contentDescription = stringResource(R.string.streak_cd),
                        tint = if (uiState.streakCount > 0) BreezeWarning else BreezeInk,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${uiState.streakCount}",
                        style = MaterialTheme.typography.labelLarge,
                        color = BreezeInk,
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(BreezePrimary)
                        .testTag("profile_button")
                        .clickable(role = Role.Button, onClick = onProfileClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = stringResource(R.string.profile_cd),
                        tint = BreezeBackground,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            // Scrollable Canvas: First viewport = Concentric Circle Only; Scroll down = Recent Spending
            val heroHeight = (viewportHeight - 76.dp).coerceAtLeast(360.dp)

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_scroll_container"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            ) {
                item(key = "concentric_hero") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(heroHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    val scale = 1f - (animatedScrollFraction * 0.14f)
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = 1f - (animatedScrollFraction * 0.35f)
                                    translationY = -animatedScrollFraction * 36.dp.toPx()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            ConcentricSpendingCircle(
                                totalSpentInr = uiState.totalSpentInr,
                                monthlyGoalInr = uiState.monthlyGoalInr,
                                hasMonthlyGoal = uiState.hasMonthlyGoal,
                                overallProgress = uiState.overallProgress,
                                categorySummaries = uiState.categorySummaries,
                                hasSmsPermission = uiState.hasSmsPermission,
                                onCircleClick = onCircleClick,
                                onSubtleAddClick = onSubtleAddClick,
                                onSetGoalClick = onSetGoalClick,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        // Subtle scroll-down hint at bottom of hero stage
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 14.dp)
                                .graphicsLayer {
                                    alpha = (1f - animatedScrollFraction * 1.8f).coerceIn(0f, 1f)
                                    translationY = cueOffsetY
                                }
                                .clip(RoundedCornerShape(50))
                                .background(BreezeSurface2)
                                .border(1.dp, BreezeLine, RoundedCornerShape(50))
                                .testTag("scroll_recent_spending_cue")
                                .clickable(role = Role.Button) {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(1)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (uiState.transactions.isEmpty()) {
                                    "Scroll for Recent Spending"
                                } else {
                                    "Recent Spending (${uiState.transactions.size})"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = BreezeInk2,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null,
                                tint = BreezeInk2,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                // Recent Spending Header with smooth reveal animation
                item(key = "recent_spending_header") {
                    AnimatedVisibility(
                        visible = recentSectionVisible || uiState.transactions.isNotEmpty(),
                        enter = fadeIn(tween(280)) +
                            slideInVertically(
                                initialOffsetY = { it / 2 },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                            ) + expandVertically(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 12.dp)
                                .testTag("recent_spending_section"),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = "LIVE INBOX FEED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BreezeInk2,
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Recent Spending",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = BreezeInk,
                                    )
                                }

                                if (BuildConfig.ENABLE_TESTING_TOOLS) {
                                    // One-tap emulator SMS simulator button (Tester build only)
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(BreezeSurface2)
                                            .border(1.dp, BreezeLine, RoundedCornerShape(50))
                                            .testTag("simulate_sms_home_button")
                                            .clickable(role = Role.Button) { onSimulateBankSms(3) }
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.Bolt,
                                            contentDescription = null,
                                            tint = BreezeSuccess,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Simulate +3 SMS",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = BreezeInk,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (uiState.transactions.isEmpty()) {
                    item(key = "recent_empty_card") {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.95f),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(BreezeSurface2)
                                    .border(1.dp, BreezeLine, RoundedCornerShape(22.dp))
                                    .padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    imageVector = AppIcons.Sms,
                                    contentDescription = null,
                                    tint = BreezeInk2,
                                    modifier = Modifier.size(26.dp),
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No recent spending yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BreezeInk,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (BuildConfig.ENABLE_TESTING_TOOLS) {
                                        "SmsContentObserverService is watching content://sms for 'Debited', 'Spent', and 'INR' patterns."
                                    } else {
                                        "FinTracker is ready to automatically log Indian bank SMS and UPI debit alerts on-device."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BreezeInk2,
                                )
                                if (BuildConfig.ENABLE_TESTING_TOOLS) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Box(
                                        modifier = Modifier
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(BreezePrimary)
                                            .clickable(role = Role.Button) { onSimulateBankSms(3) }
                                            .padding(horizontal = 18.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "Send 3 Test Bank SMS Now",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = BreezeBackground,
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val recentItems = uiState.transactions.take(15)
                    itemsIndexed(
                        items = recentItems,
                        key = { _, tx -> tx.id },
                    ) { index, tx ->
                        val category = ExpenseCategory.fromName(tx.categoryName)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(
                                    fadeInSpec = tween(durationMillis = 260),
                                    placementSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                )
                                .padding(bottom = 10.dp),
                        ) {
                            RecentSpendingCard(
                                tx = tx,
                                index = index,
                                category = category,
                                onClick = { onCategoryClick(category) },
                            )
                        }
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(36.dp))
                }
            }
        }

        // Transient floating status pill (only appears briefly after actions)
        AnimatedVisibility(
            visible = uiState.statusBannerMessage != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 74.dp, start = 20.dp, end = 20.dp),
        ) {
            uiState.statusBannerMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(BreezeInverse)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelMedium,
                        color = BreezeBackground,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentSpendingCard(
    tx: TransactionEntity,
    index: Int,
    category: ExpenseCategory,
    onClick: () -> Unit,
) {
    val sourceBadge = TransactionSource.entries
        .firstOrNull { it.name == tx.sourceName }
        ?.badgeLabel ?: "SMS"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BreezeBackground)
            .border(1.dp, BreezeLine, RoundedCornerShape(18.dp))
            .testTag("recent_spending_item_$index")
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BreezeSurface2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.displayName,
                tint = category.ringColor,
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
                text = "${category.shortName} · ${InrFormatter.formatTimestamp(tx.timestamp)} · $sourceBadge",
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
