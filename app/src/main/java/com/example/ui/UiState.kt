package com.example.ui

import com.example.capture.SmsInboxScanReport
import com.example.capture.SmsObserverStatus
import com.example.capture.SmsParseTrace
import com.example.data.TransactionEntity
import com.example.data.UserPrefs
import com.example.domain.ExpenseCategory

data class CategorySummary(
    val category: ExpenseCategory,
    val spentInr: Double,
    val budgetLimitInr: Double,
    val transactionCount: Int,
    val shareOfTotal: Float,
    val budgetProgress: Float,
    val topMerchants: List<Pair<String, Double>>,
)

data class DailySpendPoint(
    val dayOfMonth: Int,
    val label: String,
    val amountInr: Double,
    val isOverPace: Boolean,
)

data class StreakDayDot(
    val label: String,
    val spentInr: Double,
    val onPace: Boolean,
    val hasActivity: Boolean,
)

data class FinTrackerUiState(
    val monthKey: String = "",
    val monthTitle: String = "",
    val monthlyGoalInr: Double = 0.0,
    val hasMonthlyGoal: Boolean = false,
    val showMonthlyGoalPrompt: Boolean = false,
    val suggestedGoalInr: Double = 50_000.0,
    val totalSpentInr: Double = 0.0,
    val remainingInr: Double = 0.0,
    val safeDailySpendInr: Double = 0.0,
    val overallProgress: Float = 0f,
    val transactions: List<TransactionEntity> = emptyList(),
    val categorySummaries: List<CategorySummary> = emptyList(),
    val dailySeries: List<DailySpendPoint> = emptyList(),
    val streakCount: Int = 0,
    val streakDots: List<StreakDayDot> = emptyList(),
    val prefs: UserPrefs = UserPrefs(),
    val hasSmsPermission: Boolean = false,
    val isNotificationListenerEnabled: Boolean = false,
    val smsObserverStatus: SmsObserverStatus = SmsObserverStatus(),
    val isScanningSms: Boolean = false,
    val lastScanReport: SmsInboxScanReport? = null,
    val parserLabInput: String = "",
    val parserLabTrace: SmsParseTrace? = null,
    val statusBannerMessage: String? = null,
)
