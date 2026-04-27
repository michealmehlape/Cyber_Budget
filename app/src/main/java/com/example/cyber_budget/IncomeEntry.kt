package com.example.cyber_budget

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "income_entries")
data class IncomeEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val source: String,      // e.g., "Salary", "Freelance"
    val amount: Double,
    val date: String        // "yyyy-MM-dd"
)
