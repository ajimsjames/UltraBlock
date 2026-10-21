package com.ultrablock.app.ui

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ultrablock.app.R
import com.ultrablock.app.data.QueryLogItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QueryLogAdapter(
    private val items: MutableList<QueryLogItem>,
    private val onItemClick: (QueryLogItem) -> Unit
) : RecyclerView.Adapter<QueryLogAdapter.ViewHolder>() {

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

        val context = holder.itemView.context
        val badgeDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 5f * holder.itemView.resources.displayMetrics.density
        }

        if (item.isBlocked) {
            holder.txtBadge.text = "SINKHOLED"
            holder.txtBadge.setTextColor(ContextCompat.getColor(context, R.color.badge_blocked_text))
            badgeDrawable.setColor(ContextCompat.getColor(context, R.color.badge_blocked_bg))
        } else {
            holder.txtBadge.text = "ALLOWED"
            holder.txtBadge.setTextColor(ContextCompat.getColor(context, R.color.badge_allowed_text))
            badgeDrawable.setColor(ContextCompat.getColor(context, R.color.badge_allowed_bg))
        }
        holder.txtBadge.background = badgeDrawable

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<QueryLogItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
