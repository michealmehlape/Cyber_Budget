package com.example.cyber_budget

import androidx.room.*

@Dao
interface ExpenseEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntry): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntry)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntry)

    @Query("""
        SELECT * FROM expense_entries 
        WHERE userId = :userId AND date BETWEEN :startDate AND :endDate 
        ORDER BY date DESC
    """)
    suspend fun getExpensesByPeriod(userId: Int, startDate: String, endDate: String): List<ExpenseEntry>

    @Query("SELECT SUM(amount) FROM expense_entries WHERE userId = :userId AND date LIKE :month || '%'")
    suspend fun getTotalExpensesForMonth(userId: Int, month: String): Double?

    @Query("SELECT * FROM expense_entries WHERE userId = :userId ORDER BY date DESC LIMIT 3")
    suspend fun getRecentExpenses(userId: Int): List<ExpenseEntry>

    @Query("SELECT * FROM expense_entries WHERE id = :id")
    suspend fun getExpenseById(id: Int): ExpenseEntry?
}
