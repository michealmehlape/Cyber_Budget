package com.example.cyber_budget

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_entries")
data class ExpenseEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val categoryId: Int,
    val date: String,         // store as "yyyy-MM-dd"
    val startTime: String,    // "HH:mm"
    val endTime: String,      // "HH:mm"
    val description: String,
    val amount: Double,       // ← ADD THIS
    val photoPath: String? = null
)