package com.example.cyber_budget

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity representing an individual expense.
 * Persisted using the Room Persistence Library.
 * 
 * References:
 * - Room Entities: https://developer.android.com/training/data-storage/room/defining-data
 */
@Entity(tableName = "expense_entries")
data class ExpenseEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val categoryId: Int,
    val date: String,         // ISO 8601 format: yyyy-MM-dd
    val startTime: String,    // 24h format: HH:mm
    val endTime: String,
    val description: String,
    val amount: Double,
    val photoPath: String? = null // Relative or absolute path to the locally stored receipt image
)