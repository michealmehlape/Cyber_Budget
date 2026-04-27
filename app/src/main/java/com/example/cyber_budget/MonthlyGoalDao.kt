package com.example.cyber_budget

import androidx.room.*

@Dao
interface MonthlyGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: MonthlyGoal): Long

    @Update
    suspend fun updateGoal(goal: MonthlyGoal)

    // Filter by userId so users only see their own monthly goals
    @Query("SELECT * FROM monthly_goals WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getGoalForMonth(userId: Int, date: String): MonthlyGoal?

    @Query("SELECT * FROM monthly_goals WHERE userId = :userId ORDER BY date DESC")
    suspend fun getAllGoalsForUser(userId: Int): List<MonthlyGoal>
}
