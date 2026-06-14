package com.example.cyber_budget

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

/**
 * Adapter class for the Transactions RecyclerView.
 * Updated to display category-specific color dots instead of icons.
 */
class TransactionAdapter(
    private val transactions: List<TransactionUI>,
    private val onDeleteClick: (TransactionUI) -> Unit,
    private val onViewPhotoClick: (String) -> Unit = {}
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    private val TAG = "TransactionAdapter_Log"

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDesc: TextView = view.findViewById(R.id.tv_transaction_desc)
        val tvDate: TextView = view.findViewById(R.id.tv_transaction_date)
        val tvAmount: TextView = view.findViewById(R.id.tv_transaction_amount)
        val colorDot: View = view.findViewById(R.id.color_dot)
        val btnDelete: ImageView = view.findViewById(R.id.btn_delete)
        val btnViewPhoto: ImageView? = view.findViewById(R.id.btn_view_photo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions[position]

        // Show description (or category if description is empty)
        holder.tvDesc.text = transaction.description.ifEmpty { transaction.categoryName }
        holder.tvDate.text = transaction.date
        
        // Set category color dot
        try {
            holder.colorDot.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(transaction.categoryColor))
        } catch (e: Exception) {
            // Default colors for common items or fallback
            val fallbackColor = if (transaction.isIncome) "#4CAF50" else "#808080"
            holder.colorDot.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(fallbackColor))
        }
        
        if (transaction.isIncome) {
            holder.tvAmount.text = "+R ${String.format(Locale.US, "%.2f", transaction.amount)}"
            holder.tvAmount.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            holder.tvAmount.text = "-R ${String.format(Locale.US, "%.2f", transaction.amount)}"
            holder.tvAmount.setTextColor(Color.parseColor("#F44336"))
        }

        if (transaction.hasPhoto && transaction.photoPath != null) {
            holder.btnViewPhoto?.visibility = View.VISIBLE
            holder.btnViewPhoto?.setOnClickListener { onViewPhotoClick(transaction.photoPath) }
        } else {
            holder.btnViewPhoto?.visibility = View.GONE
        }

        holder.btnDelete.setOnClickListener { onDeleteClick(transaction) }
    }

    override fun getItemCount() = transactions.size
}
