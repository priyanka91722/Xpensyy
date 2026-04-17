package com.example.xpensy.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.xpensy.data.ExpenseRepository
import com.example.xpensy.model.CategoryTotal

class AnalyticsViewModel(
    repository: ExpenseRepository
) : ViewModel() {

    val categoryTotals: LiveData<List<CategoryTotal>> = repository.categoryTotals
}
