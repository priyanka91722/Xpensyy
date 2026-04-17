package com.example.xpensy.data

import androidx.lifecycle.LiveData
import com.example.xpensy.model.CategoryTotal
import com.example.xpensy.model.Expense

class ExpenseRepository(private val expenseDao: ExpenseDao) {
    val expenses: LiveData<List<Expense>> = expenseDao.getAllExpenses()
    val categoryTotals: LiveData<List<CategoryTotal>> = expenseDao.getCategoryTotals()

    suspend fun addExpense(expense: Expense) {
        expenseDao.insertExpense(expense)
    }
}
