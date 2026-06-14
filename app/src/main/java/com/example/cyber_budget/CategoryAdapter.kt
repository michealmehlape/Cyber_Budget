package com.example.cyber_budget

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Modern Adapter for displaying category names with edit and delete actions.
 * Updated to display the category's assigned color dot.
 */
class CategoryAdapter(
    private val categories: List<Category>,
    private val onEditClick: (Category) -> Unit,
    private val onDeleteClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_category_name)
        val colorDot: View = view.findViewById(R.id.color_dot)
        val btnEdit: ImageView = view.findViewById(R.id.btn_edit_category)
        val btnDelete: ImageView = view.findViewById(R.id.btn_delete_category)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.tvName.text = category.name
        
        try {
            holder.colorDot.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(category.color))
        } catch (e: Exception) {
            holder.colorDot.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.BLUE)
        }
        
        holder.btnEdit.setOnClickListener { onEditClick(category) }
        holder.btnDelete.setOnClickListener { onDeleteClick(category) }
    }

    override fun getItemCount() = categories.size
}
