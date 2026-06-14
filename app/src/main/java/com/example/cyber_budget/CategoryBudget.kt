package com.example.cyber_budget

/**
 * Model representing a budget limit for a specific category and month.
 * Persisted in Cloud Firestore under the 'category_budgets' collection.
 * 
 * Updated to support minimum and maximum goals for POE requirements.
 */
data class CategoryBudget(
    val userId: String = "",
    val categoryId: String = "",
    val month: String = "", // Format: "yyyy-MM"
    val budgetAmount: Double = 0.0,
    val minGoal: Double = 0.0, // Minimum intended spending goal
    val maxGoal: Double = 0.0  // Maximum intended spending goal (cap)
)
