package com.example.xpensy.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.xpensy.model.Expense
import com.example.xpensy.model.CategoryTotal

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY id DESC")
    fun getAllExpenses(): LiveData<List<Expense>>

    @Query(
        "SELECT category AS category, SUM(amount) AS total " +
            "FROM expenses GROUP BY category ORDER BY total DESC"
    )
    fun getCategoryTotals(): LiveData<List<CategoryTotal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)
}
