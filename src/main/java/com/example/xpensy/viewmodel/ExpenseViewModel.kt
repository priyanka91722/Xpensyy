package com.example.xpensy.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xpensy.data.ExpenseRepository
import com.example.xpensy.model.Expense
import com.example.xpensy.model.ExpenseCategory
import kotlinx.coroutines.launch

class ExpenseViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    val expenses: LiveData<List<Expense>> = repository.expenses

    fun addExpense(
        title: String,
        amount: Double,
        category: ExpenseCategory,
        latitude: Double? = null,
        longitude: Double? = null,
        locationText: String? = null
    ) {
        viewModelScope.launch {
            repository.addExpense(
                Expense(
                    title = title,
                    amount = amount,
                    category = category,
                    latitude = latitude,
                    longitude = longitude,
                    locationText = locationText
                )
            )
        }
    }
}
