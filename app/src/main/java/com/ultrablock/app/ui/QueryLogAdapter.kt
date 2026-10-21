package com.ultrablock.app.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ultrablock.app.R
import com.ultrablock.app.data.QueryLogItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QueryLogAdapter(private val items: MutableList<QueryLogItem>) :
    RecyclerView.Adapter<QueryLogAdapter.ViewHolder>() {

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtBadge: TextView = view.findViewById(R.id.txtStatusBadge)
        val txtDomain: TextView = view.findViewById(R.id.txtQueryDomain)
        val txtApp: TextView = view.findViewById(R.id.txtQueryApp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_query_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtDomain.text = item.domain
        val timeStr = timeFormat.format(Date(item.timestamp))
        val appInfo = item.packageName ?: "System / Background Service"
        holder.txtApp.text = "$appInfo • $timeStr"

        val badgeDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 5f * holder.itemView.resources.displayMetrics.density
        }

        if (item.isBlocked) {
            holder.txtBadge.text = "SINKHOLED"
            holder.txtBadge.setTextColor(Color.parseColor("#F87171"))
            badgeDrawable.setColor(Color.parseColor("#1FEF4444"))
            badgeDrawable.setStroke(1, Color.parseColor("#4DE879F9"))
        } else {
            holder.txtBadge.text = "ALLOWED"
            holder.txtBadge.setTextColor(Color.parseColor("#CBD5E1"))
            badgeDrawable.setColor(Color.parseColor("#14FFFFFF"))
            badgeDrawable.setStroke(1, Color.parseColor("#26FFFFFF"))
        }
        holder.txtBadge.background = badgeDrawable
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<QueryLogItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
