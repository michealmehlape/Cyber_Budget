package com.example.cyber_budget

data class TransactionUI(
    val id: Int,
    val description: String,
    val categoryName: String,
    val amount: Double,
    val date: String,
    val time: String,
    val isIncome: Boolean,
    val hasPhoto: Boolean = false,
    val photoPath: String? = null
)
