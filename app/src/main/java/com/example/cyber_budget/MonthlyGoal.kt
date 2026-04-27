package com.example.cyber_budget

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_goals")
data class MonthlyGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val date: String,        // store as "yyyy-MM" e.g. "2026-04"
    val minGoal: Double,
    val maxGoal: Double
)
