package com.example.xpensy.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: ExpenseCategory,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationText: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun formattedAmount(): String {
        return "Rs. %.2f".format(amount)
    }
}
