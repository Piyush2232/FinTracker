package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionSource(val badgeLabel: String) {
    SMS_INBOX("SMS"),
    SMS_LIVE("SMS Live"),
    AUTO_NOTIFICATION("Alert"),
    CSV_IMPORT("CSV"),
    SMART_PASTE("Parsed"),
    MANUAL("Manual"),
}

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["dedupeHash"], unique = true),
        Index(value = ["timestamp"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountInr: Double,
    val merchant: String,
    val rawSnippet: String,
    val categoryName: String,
    val timestamp: Long,
    val sourceName: String,
    val mlConfidence: Float,
    val isUserCorrected: Boolean = false,
    val dedupeHash: String,
)

@Entity(tableName = "monthly_budgets")
data class MonthlyBudgetEntity(
    @PrimaryKey val monthKey: String,
    val totalGoalInr: Double,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "category_budgets",
    indices = [Index(value = ["monthKey", "categoryName"], unique = true)],
)
data class CategoryBudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthKey: String,
    val categoryName: String,
    val limitInr: Double,
)

@Entity(
    tableName = "ml_token_weights",
    primaryKeys = ["token", "categoryName"],
)
data class MlTokenWeightEntity(
    val token: String,
    val categoryName: String,
    val weight: Double,
    val hitCount: Int,
)
