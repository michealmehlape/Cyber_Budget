package com.example.cyber_budget

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Data model for system and budget-related notifications.
 */
data class NotificationItem(val title: String, val message: String, val isAlert: Boolean = false)

/**
 * Adapter class for the Notifications RecyclerView.
 * Updated for the modern UI and theme consistency.
 */
class NotificationAdapter(private val notifications: List<NotificationItem>) :
    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_notif_title)
        val tvMessage: TextView = view.findViewById(R.id.tv_notif_message)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = notifications[position]
        
        holder.tvTitle.text = item.title
        holder.tvMessage.text = item.message
        
        // Use theme-consistent colors
        if (item.isAlert) {
            holder.tvTitle.setTextColor(Color.parseColor("#F44336")) // Accent Red
        } else {
            holder.tvTitle.setTextColor(Color.parseColor("#5C79E0")) // Primary Blue
        }
    }

    override fun getItemCount() = notifications.size
}
