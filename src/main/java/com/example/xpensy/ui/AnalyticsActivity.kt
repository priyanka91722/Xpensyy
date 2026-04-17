package com.example.xpensy.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.xpensy.data.ExpenseDatabase
import com.example.xpensy.data.ExpenseRepository
import com.example.xpensy.databinding.ActivityAnalyticsBinding
import com.example.xpensy.model.CategoryTotal
import com.example.xpensy.viewmodel.AnalyticsViewModel
import com.example.xpensy.viewmodel.AnalyticsViewModelFactory
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalyticsBinding
    private lateinit var viewModel: AnalyticsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViewModel()
        setupChart()
        observeAnalytics()
    }

    private fun setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener { finish() }
    }

    private fun setupViewModel() {
        val database = ExpenseDatabase.getDatabase(applicationContext)
        val repository = ExpenseRepository(database.expenseDao())
        val factory = AnalyticsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[AnalyticsViewModel::class.java]
    }

    private fun setupChart() = with(binding.pieChart) {
        description.isEnabled = false
        legend.isEnabled = true
        setUsePercentValues(true)
        setEntryLabelColor(Color.WHITE)
        setEntryLabelTextSize(12f)
        setDrawHoleEnabled(true)
        holeRadius = 58f
        transparentCircleRadius = 62f
        setHoleColor(Color.WHITE)
        centerText = getString(com.example.xpensy.R.string.chart_center_text)
        setCenterTextSize(16f)
    }

    private fun observeAnalytics() {
        viewModel.categoryTotals.observe(this) { totals ->
            binding.emptyStateText.visibility = if (totals.isEmpty()) View.VISIBLE else View.GONE
            binding.chartCard.visibility = if (totals.isEmpty()) View.GONE else View.VISIBLE

            if (totals.isNotEmpty()) {
                renderChart(totals)
            }
        }
    }

    private fun renderChart(totals: List<CategoryTotal>) {
        val entries = totals.map { PieEntry(it.total.toFloat(), it.category.displayName) }
        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.parseColor("#1565C0"),
                Color.parseColor("#2E7D32"),
                Color.parseColor("#EF6C00"),
                Color.parseColor("#8E24AA"),
                Color.parseColor("#D81B60"),
                Color.parseColor("#00838F")
            )
            sliceSpace = 3f
            valueTextColor = Color.WHITE
            valueTextSize = 12f
        }

        binding.pieChart.data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(binding.pieChart))
        }
        binding.pieChart.invalidate()
    }
}
