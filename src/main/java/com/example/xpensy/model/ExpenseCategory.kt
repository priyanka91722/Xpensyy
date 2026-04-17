package com.example.xpensy.model

enum class ExpenseCategory(val displayName: String) {
    FOOD("Food"),
    TRAVEL("Travel"),
    SHOPPING("Shopping"),
    BILLS("Bills"),
    HEALTH("Health"),
    OTHER("Other");

    override fun toString(): String = displayName
}
