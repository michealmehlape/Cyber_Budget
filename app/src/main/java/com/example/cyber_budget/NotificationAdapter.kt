package com.example.cyber_budget

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class NotificationItem(val title: String, val message: String, val isAlert: Boolean = false)

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
        
        if (item.isAlert) {
            holder.tvTitle.setTextColor(0xFFFF4444.toInt()) // Red for alerts
        } else {
            holder.tvTitle.setTextColor(0xFF3450A1.toInt()) // Blue for reminders
        }
    }

    override fun getItemCount() = notifications.size
}
