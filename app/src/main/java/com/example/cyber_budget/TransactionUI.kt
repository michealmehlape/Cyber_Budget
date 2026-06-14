package com.example.cyber_budget

/**
 * UI Model for transaction entries.
 * Updated to include the category's assigned color for consistent visual identification.
 */
data class TransactionUI(
    val id: String, // Document ID from Firestore
    val description: String,
    val categoryName: String,
    val amount: Double,
    val date: String,
    val time: String,
    val isIncome: Boolean,
    val hasPhoto: Boolean = false,
    val photoPath: String? = null,
    val categoryColor: String = "#000000" // Hex color string
)
