package com.example.cyber_budget

/**
 * Model representing a spending category.
 */
data class Category(
    val id: String = "", // Firestore Document ID
    val name: String,
    val userId: String = "",
    val color: String = "#000000" // Hex color string, default to black
)
