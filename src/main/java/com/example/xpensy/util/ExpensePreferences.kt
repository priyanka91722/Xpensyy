package com.example.xpensy.util

import android.content.Context

class ExpensePreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDailyLimit(): Double? {
        val storedValue = preferences.getFloat(KEY_DAILY_LIMIT, -1f)
        return if (storedValue > 0f) storedValue.toDouble() else null
    }

    fun setDailyLimit(limit: Double) {
        preferences.edit().putFloat(KEY_DAILY_LIMIT, limit.toFloat()).apply()
    }

    fun getLastNotifiedDay(): String? = preferences.getString(KEY_LAST_NOTIFIED_DAY, null)

    fun setLastNotifiedDay(dayKey: String) {
        preferences.edit().putString(KEY_LAST_NOTIFIED_DAY, dayKey).apply()
    }

    companion object {
        private const val PREFS_NAME = "xpensy_preferences"
        private const val KEY_DAILY_LIMIT = "daily_limit"
        private const val KEY_LAST_NOTIFIED_DAY = "last_notified_day"
    }
}
