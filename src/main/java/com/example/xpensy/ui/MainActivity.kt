package com.example.xpensy.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.xpensy.data.ExpenseDatabase
import com.example.xpensy.data.ExpenseRepository
import com.example.xpensy.databinding.ActivityMainBinding
import com.example.xpensy.databinding.DialogAddExpenseBinding
import com.example.xpensy.databinding.DialogSetLimitBinding
import com.example.xpensy.model.Expense
import com.example.xpensy.model.ExpenseCategory
import com.example.xpensy.util.ExpenseNotificationHelper
import com.example.xpensy.util.ExpensePreferences
import com.example.xpensy.viewmodel.ExpenseViewModel
import com.example.xpensy.viewmodel.ExpenseViewModelFactory
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ExpenseViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var expensePreferences: ExpensePreferences
    private lateinit var notificationHelper: ExpenseNotificationHelper
    private val expenseAdapter = ExpenseAdapter()
    private var pendingExpenseInput: PendingExpenseInput? = null
    private var pendingLocationDialog: androidx.appcompat.app.AlertDialog? = null

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val pendingInput = pendingExpenseInput ?: return@registerForActivityResult
            val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (isGranted) {
                saveExpenseWithCurrentLocation(pendingInput)
            } else {
                viewModel.addExpense(
                    title = pendingInput.title,
                    amount = pendingInput.amount,
                    category = pendingInput.category,
                    locationText = getString(com.example.xpensy.R.string.location_unavailable)
                )
                Toast.makeText(
                    this,
                    getString(com.example.xpensy.R.string.location_permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
                pendingExpenseInput = null
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(
                    this,
                    getString(com.example.xpensy.R.string.notification_permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        expensePreferences = ExpensePreferences(this)
        notificationHelper = ExpenseNotificationHelper(this)
        notificationHelper.createChannel()
        setupViewModel()
        setupRecyclerView()
        updateDailyLimitText()
        observeExpenses()
        setupActions()
    }

    private fun setupViewModel() {
        val database = ExpenseDatabase.getDatabase(applicationContext)
        val repository = ExpenseRepository(database.expenseDao())
        val factory = ExpenseViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ExpenseViewModel::class.java]
    }

    private fun setupRecyclerView() = with(binding.expenseRecyclerView) {
        layoutManager = LinearLayoutManager(this@MainActivity)
        adapter = expenseAdapter
    }

    private fun observeExpenses() {
        viewModel.expenses.observe(this) { expenses ->
            expenseAdapter.submitList(expenses)
            binding.countValueText.text = expenses.size.toString()
            binding.emptyStateText.visibility = if (expenses.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
            checkDailyLimitAndNotify(expenses)
        }
    }

    private fun setupActions() {
        binding.addExpenseFab.setOnClickListener { showAddExpenseDialog() }
        binding.addExpenseInlineButton.setOnClickListener { showAddExpenseDialog() }
        binding.setLimitButton.setOnClickListener { showSetLimitDialog() }
        binding.openEmiCalculatorButton.setOnClickListener {
            startActivity(Intent(this, EmiCalculatorActivity::class.java))
        }
        binding.openAnalyticsButton.setOnClickListener {
            startActivity(Intent(this, AnalyticsActivity::class.java))
        }
    }

    private fun showSetLimitDialog() {
        val dialogBinding = DialogSetLimitBinding.inflate(LayoutInflater.from(this))
        val existingLimit = expensePreferences.getDailyLimit()
        if (existingLimit != null) {
            dialogBinding.limitEditText.setText(existingLimit.toString())
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(com.example.xpensy.R.string.set_daily_limit_title))
            .setView(dialogBinding.root)
            .setNegativeButton(getString(com.example.xpensy.R.string.action_cancel), null)
            .setPositiveButton(getString(com.example.xpensy.R.string.action_save), null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val limit = dialogBinding.limitEditText.text?.toString()?.trim().orEmpty().toDoubleOrNull()
                dialogBinding.limitLayout.error = null

                if (limit == null || limit <= 0.0) {
                    dialogBinding.limitLayout.error = getString(com.example.xpensy.R.string.daily_limit_invalid)
                    return@setOnClickListener
                }

                expensePreferences.setDailyLimit(limit)
                updateDailyLimitText()
                Toast.makeText(this, getString(com.example.xpensy.R.string.daily_limit_saved), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                viewModel.expenses.value?.let { checkDailyLimitAndNotify(it) }
            }
        }

        dialog.show()
    }

    private fun showAddExpenseDialog() {
        val dialogBinding = DialogAddExpenseBinding.inflate(LayoutInflater.from(this))
        setupCategoryDropdown(dialogBinding)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(com.example.xpensy.R.string.add_expense_dialog_title))
            .setView(dialogBinding.root)
            .setNegativeButton(getString(com.example.xpensy.R.string.action_cancel), null)
            .setPositiveButton(getString(com.example.xpensy.R.string.action_save), null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (validateAndSaveExpense(dialogBinding)) {
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun setupCategoryDropdown(dialogBinding: DialogAddExpenseBinding) {
        val categories = ExpenseCategory.entries.toTypedArray()
        val categoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            categories
        )

        dialogBinding.categoryAutoComplete.setAdapter(categoryAdapter)
        dialogBinding.categoryAutoComplete.setText("", false)
        dialogBinding.categoryAutoComplete.setOnClickListener {
            dialogBinding.categoryAutoComplete.showDropDown()
        }
        dialogBinding.categoryAutoComplete.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                dialogBinding.categoryAutoComplete.showDropDown()
            }
        }
    }

    private fun validateAndSaveExpense(dialogBinding: DialogAddExpenseBinding): Boolean {
        val title = dialogBinding.titleEditText.text?.toString()?.trim().orEmpty()
        val amountText = dialogBinding.amountEditText.text?.toString()?.trim().orEmpty()
        val categoryText = dialogBinding.categoryAutoComplete.text?.toString()?.trim().orEmpty()
        val selectedCategory = ExpenseCategory.entries.firstOrNull {
            it.displayName.equals(categoryText, ignoreCase = true)
        }
        val amount = amountText.toDoubleOrNull()

        dialogBinding.titleLayout.error = null
        dialogBinding.amountLayout.error = null
        dialogBinding.categoryLayout.error = null

        var isValid = true

        if (title.isBlank()) {
            dialogBinding.titleLayout.error = getString(com.example.xpensy.R.string.error_title_required)
            isValid = false
        }

        if (amount == null || amount <= 0.0) {
            dialogBinding.amountLayout.error = getString(com.example.xpensy.R.string.error_amount_required)
            isValid = false
        }

        if (selectedCategory == null) {
            dialogBinding.categoryLayout.error = getString(com.example.xpensy.R.string.error_category_required)
            isValid = false
        }

        if (!isValid) {
            return false
        }

        val pendingInput = PendingExpenseInput(
            title = title,
            amount = amount!!,
            category = selectedCategory!!
        )
        saveExpenseWithLocationIfPossible(pendingInput)
        return true
    }

    private fun saveExpenseWithLocationIfPossible(input: PendingExpenseInput) {
        pendingExpenseInput = input
        if (hasLocationPermission()) {
            saveExpenseWithCurrentLocation(input)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun saveExpenseWithCurrentLocation(input: PendingExpenseInput) {
        showLocationLoadingDialog()

        val cancellationTokenSource = CancellationTokenSource()
        fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    persistExpenseWithLocation(input, location)
                } else {
                    fallbackToLastKnownLocation(input)
                }
            }
            .addOnFailureListener {
                fallbackToLastKnownLocation(input)
            }
    }

    private fun fallbackToLastKnownLocation(input: PendingExpenseInput) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                dismissLocationLoadingDialog()
                if (location != null) {
                    persistExpenseWithLocation(input, location)
                } else {
                    persistExpenseWithoutCoordinates(input)
                }
            }
            .addOnFailureListener {
                dismissLocationLoadingDialog()
                persistExpenseWithoutCoordinates(input)
            }
    }

    private fun persistExpenseWithLocation(input: PendingExpenseInput, location: Location) {
        dismissLocationLoadingDialog()
        val locationText = resolveLocationText(location)
        viewModel.addExpense(
            title = input.title,
            amount = input.amount,
            category = input.category,
            latitude = location.latitude,
            longitude = location.longitude,
            locationText = locationText
        )
        Toast.makeText(this, getString(com.example.xpensy.R.string.expense_added), Toast.LENGTH_SHORT).show()
        pendingExpenseInput = null
    }

    private fun persistExpenseWithoutCoordinates(input: PendingExpenseInput) {
        viewModel.addExpense(
            title = input.title,
            amount = input.amount,
            category = input.category,
            locationText = getString(com.example.xpensy.R.string.location_unavailable)
        )
        Toast.makeText(this, getString(com.example.xpensy.R.string.expense_added), Toast.LENGTH_SHORT).show()
        pendingExpenseInput = null
    }

    private fun resolveLocationText(location: Location): String {
        val geocoder = Geocoder(this, Locale.getDefault())
        return try {
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            val address = addresses?.firstOrNull()
            address?.locality
                ?: address?.subAdminArea
                ?: address?.adminArea
                ?: "Lat %.4f, Lng %.4f".format(location.latitude, location.longitude)
        } catch (_: IOException) {
            "Lat %.4f, Lng %.4f".format(location.latitude, location.longitude)
        } catch (_: IllegalArgumentException) {
            getString(com.example.xpensy.R.string.location_city_unknown)
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showLocationLoadingDialog() {
        dismissLocationLoadingDialog()
        pendingLocationDialog = MaterialAlertDialogBuilder(this)
            .setMessage(getString(com.example.xpensy.R.string.location_fetching))
            .setCancelable(false)
            .create()
        pendingLocationDialog?.show()
    }

    private fun dismissLocationLoadingDialog() {
        pendingLocationDialog?.dismiss()
        pendingLocationDialog = null
    }

    private fun updateDailyLimitText() {
        val limit = expensePreferences.getDailyLimit()
        binding.dailyLimitText.text = if (limit != null) {
            getString(com.example.xpensy.R.string.daily_limit_value, limit)
        } else {
            getString(com.example.xpensy.R.string.daily_limit_not_set)
        }
    }

    private fun checkDailyLimitAndNotify(expenses: List<Expense>) {
        val limit = expensePreferences.getDailyLimit() ?: return
        val todayExpenses = expenses.filter { isToday(it.createdAt) }
        val totalSpentToday = todayExpenses.sumOf { it.amount }
        val todayKey = currentDayKey()

        if (totalSpentToday > limit && expensePreferences.getLastNotifiedDay() != todayKey) {
            requestNotificationPermissionIfNeeded()
            if (notificationHelper.canPostNotifications()) {
                notificationHelper.showDailyLimitExceededNotification(totalSpentToday, limit)
                expensePreferences.setLastNotifiedDay(todayKey)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun isToday(timestamp: Long): Boolean {
        if (timestamp <= 0L) return false
        val zoneId = ZoneId.systemDefault()
        val expenseDate = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
        val today = Instant.now().atZone(zoneId).toLocalDate()
        return expenseDate == today
    }

    private fun currentDayKey(): String {
        return DateTimeFormatter.ISO_LOCAL_DATE.format(
            Instant.now().atZone(ZoneId.systemDefault()).toLocalDate()
        )
    }

    private data class PendingExpenseInput(
        val title: String,
        val amount: Double,
        val category: ExpenseCategory
    )
}
