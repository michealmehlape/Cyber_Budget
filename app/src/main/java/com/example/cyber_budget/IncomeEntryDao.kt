package com.example.cyber_budget

import androidx.room.*

@Dao
interface IncomeEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: IncomeEntry): Long

    @Update
    suspend fun updateIncome(income: IncomeEntry)

    @Delete
    suspend fun deleteIncome(income: IncomeEntry)

    @Query("SELECT * FROM income_entries WHERE userId = :userId ORDER BY date DESC")
    suspend fun getIncomeForUser(userId: Int): List<IncomeEntry>

    @Query("SELECT SUM(amount) FROM income_entries WHERE userId = :userId AND date LIKE :month || '%'")
    suspend fun getTotalIncomeForMonth(userId: Int, month: String): Double?
}
