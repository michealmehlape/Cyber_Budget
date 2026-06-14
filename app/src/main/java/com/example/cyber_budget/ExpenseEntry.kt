package com.example.cyber_budget

/**
 * Model representing an individual expense transaction.
 * Maps to the 'expense_entries' collection in Cloud Firestore.
 * 
 * References:
 * - Firestore Data Mapping: https://firebase.google.com/docs/firestore/manage-data/add-data
 */
data class ExpenseEntry(
    val userId: String = "",
    val categoryId: String = "",
    val date: String = "",         // Format: yyyy-MM-dd
    val startTime: String = "",    // Format: HH:mm
    val endTime: String = "",      // Format: HH:mm
    val description: String = "",
    val amount: Double = 0.0,
    val photoPath: String? = null  // Path to locally persisted receipt image
)
