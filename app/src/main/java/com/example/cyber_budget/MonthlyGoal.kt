package com.example.cyber_budget

/**
 * DEPRECATED: This model was part of the Room implementation.
 * Monthly goals are now handled through category-specific budgets in Firestore.
 */
data class MonthlyGoal(
    val id: Int = 0,
    val userId: Int = 0,
    val date: String = "",
    val minGoal: Double = 0.0,
    val maxGoal: Double = 0.0
)
