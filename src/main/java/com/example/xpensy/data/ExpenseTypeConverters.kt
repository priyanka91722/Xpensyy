package com.example.xpensy.data

import androidx.room.TypeConverter
import com.example.xpensy.model.ExpenseCategory

class ExpenseTypeConverters {

    @TypeConverter
    fun fromExpenseCategory(category: ExpenseCategory): String {
        return category.name
    }

    @TypeConverter
    fun toExpenseCategory(categoryName: String): ExpenseCategory {
        return ExpenseCategory.valueOf(categoryName)
    }
}
