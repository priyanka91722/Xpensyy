package com.example.xpensy.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.xpensy.R
import com.example.xpensy.databinding.ItemExpenseBinding
import com.example.xpensy.model.Expense

class ExpenseAdapter : ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ExpenseViewHolder(
        private val binding: ItemExpenseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(expense: Expense) = with(binding) {
            titleText.text = expense.title
            categoryText.text = expense.category.displayName
            amountText.text = expense.formattedAmount()
            locationText.text = expense.locationText ?: buildCoordinatesFallback(expense)
        }

        private fun buildCoordinatesFallback(expense: Expense): String {
            val latitude = expense.latitude
            val longitude = expense.longitude
            return if (latitude != null && longitude != null) {
                "Lat %.4f, Lng %.4f".format(latitude, longitude)
            } else {
                itemView.context.getString(R.string.location_unavailable)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Expense>() {
            override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean {
                return oldItem == newItem
            }
        }
    }
}
