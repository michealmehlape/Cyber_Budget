package com.example.cyber_budget

import androidx.room.*

@Dao
interface CategoryBudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: CategoryBudget)

    @Update
    suspend fun updateBudget(budget: CategoryBudget)

    @Query("SELECT * FROM category_budgets WHERE userId = :userId AND month = :month")
    suspend fun getBudgetsForMonth(userId: Int, month: String): List<CategoryBudget>

    @Query("SELECT * FROM category_budgets WHERE userId = :userId AND categoryId = :categoryId AND month = :month LIMIT 1")
    suspend fun getBudgetForCategory(userId: Int, categoryId: Int, month: String): CategoryBudget?
}
