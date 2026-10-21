package com.ultrablock.app.ui

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.ultrablock.app.R
import com.ultrablock.app.data.AppBlockItem
import com.ultrablock.app.dns.DomainFilter

class AppStatsAdapter(
    private var items: List<AppBlockItem>,
    private val onToggleChanged: (packageName: String, isProtected: Boolean) -> Unit
) : RecyclerView.Adapter<AppStatsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgAppIcon: ImageView = view.findViewById(R.id.imgAppIcon)
        val txtAppName: TextView = view.findViewById(R.id.txtAppName)
        val txtAppPackage: TextView = view.findViewById(R.id.txtAppPackage)
        val txtLastBlockedDomain: TextView = view.findViewById(R.id.txtLastBlockedDomain)
        val txtBlockBadge: TextView = view.findViewById(R.id.txtBlockBadge)
        val switchAppProtection: SwitchMaterial = view.findViewById(R.id.switchAppProtection)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_block, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.txtAppName.text = item.appName
        holder.txtAppPackage.text = item.packageName
        holder.txtBlockBadge.text = item.blockedCount.toString()

        if (item.packageName == "System & Background Ads") {
            holder.switchAppProtection.visibility = View.GONE
            holder.itemView.setOnClickListener(null)
        } else {
            holder.switchAppProtection.visibility = View.VISIBLE
            // Remove previous listener before setting checked state to avoid recursion
            holder.switchAppProtection.setOnCheckedChangeListener(null)
            holder.switchAppProtection.isChecked = item.isProtected

            holder.switchAppProtection.setOnCheckedChangeListener { _, isChecked ->
                item.isProtected = isChecked
                updateProtectionText(holder, isChecked, item.lastBlockedDomain)
                onToggleChanged(item.packageName, isChecked)
            }

            // TV remote: pressing OK / Center button on the focused card toggles the switch
            holder.itemView.setOnClickListener {
                holder.switchAppProtection.toggle()
            }
        }

        updateProtectionText(holder, item.isProtected, item.lastBlockedDomain)

        if (item.icon != null) {
            holder.imgAppIcon.setImageDrawable(item.icon)
            holder.imgAppIcon.imageTintList = null
        } else {
            holder.imgAppIcon.setImageResource(R.drawable.ic_tile_shield)
        }
    }

    private fun updateProtectionText(holder: ViewHolder, isProtected: Boolean, lastBlockedDomain: String) {
        if (lastBlockedDomain.isNotEmpty()) {
            holder.txtLastBlockedDomain.visibility = View.VISIBLE
            holder.txtLastBlockedDomain.text = if (isProtected) {
                "Last blocked: $lastBlockedDomain"
            } else {
                "Bypassed • Was: $lastBlockedDomain"
            }
            holder.txtLastBlockedDomain.setTextColor(
                if (isProtected) Color.parseColor("#10B981") else Color.parseColor("#EF4444")
            )
        } else {
            holder.txtLastBlockedDomain.visibility = View.VISIBLE
            if (isProtected) {
                holder.txtLastBlockedDomain.text = "Protection: Active"
                holder.txtLastBlockedDomain.setTextColor(Color.parseColor("#10B981"))
            } else {
                holder.txtLastBlockedDomain.text = "Protection: Bypassed (Off)"
                holder.txtLastBlockedDomain.setTextColor(Color.parseColor("#EF4444"))
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<AppBlockItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
