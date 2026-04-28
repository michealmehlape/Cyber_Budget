package com.example.cyber_budget

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class TransactionAdapter(
    private val transactions: List<TransactionUI>,
    private val onDeleteClick: (TransactionUI) -> Unit,
    private val onViewPhotoClick: (String) -> Unit = {}
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDesc: TextView = view.findViewById(R.id.tv_transaction_desc)
        val tvCategory: TextView = view.findViewById(R.id.tv_transaction_category)
        val tvAmount: TextView = view.findViewById(R.id.tv_transaction_amount)
        val tvTime: TextView = view.findViewById(R.id.tv_transaction_time)
        val ivIcon: ImageView = view.findViewById(R.id.iv_category_icon)
        val btnDelete: ImageView = view.findViewById(R.id.btn_delete)
        val btnViewPhoto: ImageView? = view.findViewById(R.id.btn_view_photo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions[position]

        holder.tvDesc.text = transaction.description
        holder.tvCategory.text = transaction.categoryName
        holder.tvTime.text = "${transaction.date} • ${transaction.time}"
        
        if (transaction.isIncome) {
            holder.tvAmount.text = "+R ${String.format(Locale.US, "%.2f", transaction.amount)}"
            holder.tvAmount.setTextColor(Color.parseColor("#4CAF50")) // Green
        } else {
            holder.tvAmount.text = "-R ${String.format(Locale.US, "%.2f", transaction.amount)}"
            holder.tvAmount.setTextColor(Color.parseColor("#F44336")) // Red
        }

        if (transaction.hasPhoto && transaction.photoPath != null) {
            holder.btnViewPhoto?.visibility = View.VISIBLE
            holder.btnViewPhoto?.setOnClickListener {
                onViewPhotoClick(transaction.photoPath)
            }
        } else {
            holder.btnViewPhoto?.visibility = View.GONE
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(transaction)
        }
    }

    override fun getItemCount() = transactions.size
}
