package com.example.cyber_budget

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "category_budgets",
    indices = [Index(value = ["userId", "categoryId", "month"], unique = true)]
)
data class CategoryBudget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val categoryId: Int,
    val month: String, // "yyyy-MM"
    val budgetAmount: Double
)
