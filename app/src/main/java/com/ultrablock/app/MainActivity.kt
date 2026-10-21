package com.ultrablock.app

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ultrablock.app.data.BlockStatsManager
import com.ultrablock.app.data.DnsProviderManager
import com.ultrablock.app.data.QueryLogManager
import com.ultrablock.app.data.SnoozeManager
import com.ultrablock.app.data.UpstreamDns
import com.ultrablock.app.dns.DomainFilter
import com.ultrablock.app.service.UltraVpnService
import com.ultrablock.app.ui.AppStatsActivity
import com.ultrablock.app.ui.ModulesActivity
import com.ultrablock.app.ui.QueryLogAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var btnToggleShield: FrameLayout
    private lateinit var imgShieldIcon: ImageView
    private lateinit var txtStatusTitle: TextView
    private lateinit var txtStatusSubtitle: TextView
    private lateinit var txtBlockedCount: TextView
    private lateinit var txtProcessedCount: TextView
    private lateinit var txtBlockRate: TextView
    private lateinit var btnChangeDns: TextView
    private lateinit var btnOpenAppStats: TextView
    private lateinit var btnOpenModules: TextView
    private lateinit var btnBatteryOpt: TextView
    private lateinit var btnSnooze5m: TextView
    private lateinit var btnSnooze15m: TextView
    private lateinit var btnSnooze1h: TextView
    private lateinit var txtClearLog: TextView

    private lateinit var recyclerQueryLogs: RecyclerView
    private lateinit var queryLogAdapter: QueryLogAdapter

    private val handler = Handler(Looper.getMainLooper())
    private val statsUpdater = object : Runnable {
        override fun run() {
            updateStats()
            handler.postDelayed(this, 1000)
        }
    }

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateUiState()
        }
    }

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        DomainFilter.load(applicationContext)
        BlockStatsManager.init(applicationContext)
        DnsProviderManager.init(applicationContext)
        SnoozeManager.init(applicationContext)

        btnToggleShield = findViewById(R.id.btnToggleShield)
        imgShieldIcon = findViewById(R.id.imgShieldIcon)
        txtStatusTitle = findViewById(R.id.txtStatusTitle)
        txtStatusSubtitle = findViewById(R.id.txtStatusSubtitle)
        txtBlockedCount = findViewById(R.id.txtBlockedCount)
        txtProcessedCount = findViewById(R.id.txtProcessedCount)
        txtBlockRate = findViewById(R.id.txtBlockRate)
        btnChangeDns = findViewById(R.id.btnChangeDns)
        btnOpenAppStats = findViewById(R.id.btnOpenAppStats)
        btnOpenModules = findViewById(R.id.btnOpenModules)
        btnBatteryOpt = findViewById(R.id.btnBatteryOpt)
        btnSnooze5m = findViewById(R.id.btnSnooze5m)
        btnSnooze15m = findViewById(R.id.btnSnooze15m)
        btnSnooze1h = findViewById(R.id.btnSnooze1h)
        txtClearLog = findViewById(R.id.txtClearLog)

        recyclerQueryLogs = findViewById(R.id.recyclerQueryLogs)
        recyclerQueryLogs.layoutManager = LinearLayoutManager(this)
        queryLogAdapter = QueryLogAdapter(mutableListOf())
        recyclerQueryLogs.adapter = queryLogAdapter

        btnToggleShield.setOnClickListener {
            toggleShield()
        }

        btnOpenAppStats.setOnClickListener {
            startActivity(Intent(this, AppStatsActivity::class.java))
        }

        btnOpenModules.setOnClickListener {
            startActivity(Intent(this, ModulesActivity::class.java))
        }

        btnChangeDns.setOnClickListener {
            showDnsSelectionDialog()
        }

        btnSnooze5m.setOnClickListener { snoozeShield(5) }
        btnSnooze15m.setOnClickListener { snoozeShield(15) }
        btnSnooze1h.setOnClickListener { snoozeShield(60) }

        txtClearLog.setOnClickListener {
            QueryLogManager.clear()
            queryLogAdapter.updateData(emptyList())
        }

        btnBatteryOpt.setOnClickListener {
            requestBatteryOptimizationExemption()
        }

        updateDnsButtonText()
    }

    private fun updateDnsButtonText() {
        val prov = DnsProviderManager.currentProvider
        btnChangeDns.text = "DNS: ${prov.ip} ▾"
    }

    private fun showDnsSelectionDialog() {
        val providers = UpstreamDns.values()
        val names = providers.map { "${it.displayName}\n${it.description}" }.toTypedArray()
        val currentIndex = providers.indexOf(DnsProviderManager.currentProvider)

        AlertDialog.Builder(this)
            .setTitle("Select Upstream DNS Resolver")
            .setSingleChoiceItems(names, currentIndex) { dialog, which ->
                val selected = providers[which]
                DnsProviderManager.setProvider(this, selected)
                updateDnsButtonText()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun snoozeShield(minutes: Int) {
        if (SnoozeManager.isSnoozed()) {
            SnoozeManager.cancelSnooze(this)
        } else {
            SnoozeManager.snooze(this, minutes)
        }
        updateUiState()
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (_: Exception) {
                    try {
                        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    } catch (_: Exception) {}
                }
            } else {
                btnBatteryOpt.text = "OPTIMIZED"
                btnBatteryOpt.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ContextCompat.registerReceiver(
            this,
            stateReceiver,
            IntentFilter(UltraVpnService.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        updateUiState()
        handler.post(statsUpdater)
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(stateReceiver)
        } catch (ignored: Exception) {}
        handler.removeCallbacks(statsUpdater)
    }

    private fun toggleShield() {
        val running = UltraVpnService.isRunning.get()
        if (running) {
            val stopIntent = Intent(this, UltraVpnService::class.java).apply {
                action = UltraVpnService.ACTION_STOP
            }
            startService(stopIntent)
        } else {
            val prepareIntent = VpnService.prepare(this)
            if (prepareIntent != null) {
                vpnPermissionLauncher.launch(prepareIntent)
            } else {
                startVpnService()
            }
        }
    }

    private fun startVpnService() {
        val startIntent = Intent(this, UltraVpnService::class.java).apply {
            action = UltraVpnService.ACTION_START
        }
        ContextCompat.startForegroundService(this, startIntent)
    }

    private fun updateUiState() {
        val active = UltraVpnService.isRunning.get()
        val isSnoozed = SnoozeManager.isSnoozed()
        val primaryColor = ContextCompat.getColor(this, R.color.text_primary)
        val secondaryColor = ContextCompat.getColor(this, R.color.text_secondary)

        if (isSnoozed) {
            val rem = SnoozeManager.getRemainingMinutes()
            txtStatusTitle.text = "PAUSED ($rem MIN REMAINING)"
            txtStatusTitle.setTextColor(secondaryColor)
            imgShieldIcon.imageTintList = ColorStateList.valueOf(secondaryColor)
            txtStatusSubtitle.text = "Protection temporarily paused. Tap to resume."
        } else if (active) {
            txtStatusTitle.text = getString(R.string.shield_on)
            txtStatusTitle.setTextColor(primaryColor)
            imgShieldIcon.imageTintList = ColorStateList.valueOf(primaryColor)
            txtStatusSubtitle.text = getString(R.string.status_subtitle)
        } else {
            txtStatusTitle.text = getString(R.string.shield_off)
            txtStatusTitle.setTextColor(secondaryColor)
            imgShieldIcon.imageTintList = ColorStateList.valueOf(secondaryColor)
            txtStatusSubtitle.text = getString(R.string.status_subtitle)
        }
        updateStats()
    }

    private fun updateStats() {
        val blocked = UltraVpnService.adsBlocked.get()
        val total = UltraVpnService.queriesProcessed.get()
        txtBlockedCount.text = blocked.toString()
        txtProcessedCount.text = total.toString()

        val rate = if (total > 0) ((blocked.toDouble() / total.toDouble()) * 100).toInt() else 0
        txtBlockRate.text = "$rate%"

        // Refresh live query stream
        val logs = QueryLogManager.getLogs()
        if (logs.isNotEmpty()) {
            queryLogAdapter.updateData(logs)
        }
    }
}
