package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE timestamp >= :startMillis AND timestamp < :endMillis ORDER BY timestamp DESC")
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(entities: List<TransactionEntity>): List<Long>

    @Query("UPDATE transactions SET categoryName = :categoryName, isUserCorrected = 1, mlConfidence = 1.0 WHERE id = :id")
    suspend fun updateCategory(id: Long, categoryName: String)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM monthly_budgets WHERE monthKey = :monthKey LIMIT 1")
    fun observeMonthlyBudget(monthKey: String): Flow<MonthlyBudgetEntity?>

    @Query("SELECT * FROM monthly_budgets WHERE monthKey = :monthKey LIMIT 1")
    suspend fun getMonthlyBudget(monthKey: String): MonthlyBudgetEntity?

    @Query("SELECT * FROM monthly_budgets ORDER BY monthKey DESC LIMIT 1")
    suspend fun getLatestMonthlyBudget(): MonthlyBudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMonthlyBudget(budget: MonthlyBudgetEntity)

    @Query("SELECT * FROM category_budgets WHERE monthKey = :monthKey")
    fun observeCategoryBudgets(monthKey: String): Flow<List<CategoryBudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategoryBudgets(budgets: List<CategoryBudgetEntity>)
}

@Dao
interface MlWeightDao {
    @Query("SELECT * FROM ml_token_weights")
    suspend fun getAllWeights(): List<MlTokenWeightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeights(weights: List<MlTokenWeightEntity>)

    @Query("DELETE FROM ml_token_weights")
    suspend fun clearAll()
}
