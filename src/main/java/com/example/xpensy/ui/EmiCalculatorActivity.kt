package com.example.xpensy.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.xpensy.databinding.ActivityEmiCalculatorBinding
import kotlin.math.pow

class EmiCalculatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmiCalculatorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmiCalculatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupActions()
    }

    private fun setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener { finish() }
    }

    private fun setupActions() {
        binding.calculateButton.setOnClickListener {
            calculateEmi()
        }
    }

    private fun calculateEmi() {
        val principal = binding.loanAmountEditText.text?.toString()?.trim().orEmpty().toDoubleOrNull()
        val annualRate = binding.interestRateEditText.text?.toString()?.trim().orEmpty().toDoubleOrNull()
        val years = binding.timeEditText.text?.toString()?.trim().orEmpty().toDoubleOrNull()

        binding.loanAmountLayout.error = null
        binding.interestRateLayout.error = null
        binding.timeLayout.error = null

        var isValid = true

        if (principal == null || principal <= 0.0) {
            binding.loanAmountLayout.error = getString(com.example.xpensy.R.string.emi_invalid_input)
            isValid = false
        }

        if (annualRate == null || annualRate <= 0.0) {
            binding.interestRateLayout.error = getString(com.example.xpensy.R.string.emi_invalid_input)
            isValid = false
        }

        if (years == null || years <= 0.0) {
            binding.timeLayout.error = getString(com.example.xpensy.R.string.emi_invalid_input)
            isValid = false
        }

        if (!isValid) return

        val monthlyRate = (annualRate!! / 12.0) / 100.0
        val months = years!! * 12.0
        val emi = if (monthlyRate == 0.0) {
            principal!! / months
        } else {
            val rateFactor = (1 + monthlyRate).pow(months)
            principal!! * monthlyRate * rateFactor / (rateFactor - 1)
        }

        binding.resultValueText.text = getString(com.example.xpensy.R.string.emi_result_value, emi)
        binding.resultCard.visibility = View.VISIBLE
    }
}
