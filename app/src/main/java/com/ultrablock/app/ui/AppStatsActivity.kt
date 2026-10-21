package com.ultrablock.app.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ultrablock.app.R
import com.ultrablock.app.data.BlockStatsManager
import com.ultrablock.app.dns.DomainFilter

class AppStatsActivity : AppCompatActivity() {

    private lateinit var recyclerViewApps: RecyclerView
    private lateinit var txtEmptyState: TextView
    private lateinit var txtTotalSummary: TextView
    private lateinit var btnClearStats: Button
    private lateinit var btnBack: ImageButton

    private lateinit var adapter: AppStatsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_stats)

        DomainFilter.load(applicationContext)
        BlockStatsManager.init(applicationContext)

        recyclerViewApps = findViewById(R.id.recyclerViewApps)
        txtEmptyState = findViewById(R.id.txtEmptyState)
        txtTotalSummary = findViewById(R.id.txtTotalSummary)
        btnClearStats = findViewById(R.id.btnClearStats)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        recyclerViewApps.layoutManager = LinearLayoutManager(this)
        adapter = AppStatsAdapter(emptyList()) { pkg, isProtected ->
            // Toggle per-app protection exemption
            DomainFilter.setAppExempt(this, pkg, !isProtected)
            if (pkg == "appn.chavaramatrimony") {
                DomainFilter.updateToggle(this, DomainFilter.KEY_BLOCK_CHAVARA, isProtected)
            }
        }
        recyclerViewApps.adapter = adapter

        btnClearStats.setOnClickListener {
            BlockStatsManager.resetStats(this)
            loadStats()
        }

        loadStats()
    }

    override fun onResume() {
        super.onResume()
        loadStats()
    }

    private fun loadStats() {
        val items = BlockStatsManager.getAppItems(packageManager)

        if (items.isEmpty()) {
            txtEmptyState.visibility = View.VISIBLE
            recyclerViewApps.visibility = View.GONE
            txtTotalSummary.text = "0 apps tracked"
        } else {
            txtEmptyState.visibility = View.GONE
            recyclerViewApps.visibility = View.VISIBLE
            val totalBlocks = items.sumOf { it.blockedCount }
            txtTotalSummary.text = "${items.size} apps configured • $totalBlocks ads sinkholed"
            adapter.updateList(items)
        }
    }
}
