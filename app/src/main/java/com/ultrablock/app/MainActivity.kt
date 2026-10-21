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
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ultrablock.app.data.BlockStatsManager
import com.ultrablock.app.data.CustomRuleManager
import com.ultrablock.app.data.DnsProviderManager
import com.ultrablock.app.data.OperationMode
import com.ultrablock.app.data.OperationModeManager
import com.ultrablock.app.data.QueryLogItem
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
    private lateinit var btnChangeMode: TextView
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
        CustomRuleManager.init(applicationContext)
        OperationModeManager.init(applicationContext)

        btnToggleShield = findViewById(R.id.btnToggleShield)
        imgShieldIcon = findViewById(R.id.imgShieldIcon)
        txtStatusTitle = findViewById(R.id.txtStatusTitle)
        txtStatusSubtitle = findViewById(R.id.txtStatusSubtitle)
        txtBlockedCount = findViewById(R.id.txtBlockedCount)
        txtProcessedCount = findViewById(R.id.txtProcessedCount)
        txtBlockRate = findViewById(R.id.txtBlockRate)
        btnChangeDns = findViewById(R.id.btnChangeDns)
        btnChangeMode = findViewById(R.id.btnChangeMode)
        btnOpenAppStats = findViewById(R.id.btnOpenAppStats)
        btnOpenModules = findViewById(R.id.btnOpenModules)
        btnBatteryOpt = findViewById(R.id.btnBatteryOpt)
        btnSnooze5m = findViewById(R.id.btnSnooze5m)
        btnSnooze15m = findViewById(R.id.btnSnooze15m)
        btnSnooze1h = findViewById(R.id.btnSnooze1h)
        txtClearLog = findViewById(R.id.txtClearLog)

        recyclerQueryLogs = findViewById(R.id.recyclerQueryLogs)
        recyclerQueryLogs.layoutManager = LinearLayoutManager(this)
        queryLogAdapter = QueryLogAdapter(mutableListOf()) { item ->
            showDomainActionDialog(item)
        }
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

        btnChangeMode.setOnClickListener {
            showModeSelectionDialog()
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
        updateModeButtonText()
    }

    private fun updateDnsButtonText() {
        val prov = DnsProviderManager.currentProvider
        val display = if (prov == UpstreamDns.CUSTOM) "CUSTOM: ${DnsProviderManager.customDnsIp}" else prov.displayName.substringBefore(" (")
        btnChangeDns.text = "DNS: $display ▾"
    }

    private fun updateModeButtonText() {
        val mode = OperationModeManager.currentMode
        btnChangeMode.text = if (mode == OperationMode.ROOT_HOSTS) "MODE: ROOT ▾" else "MODE: VPN ▾"
    }

    private fun showModeSelectionDialog() {
        val modes = OperationMode.values()
        val names = modes.map { "${it.title}\n${it.desc}" }.toTypedArray()
        val currentIndex = modes.indexOf(OperationModeManager.currentMode)

        AlertDialog.Builder(this)
            .setTitle("Select Operation Mode")
            .setSingleChoiceItems(names, currentIndex) { dialog, which ->
                val selected = modes[which]
                if (selected == OperationMode.ROOT_HOSTS) {
                    if (OperationModeManager.isRootAvailable()) {
                        OperationModeManager.setMode(this, selected)
                        updateModeButtonText()
                        Toast.makeText(this, "Root mode enabled (KernelSU/Magisk)", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Root access (su) not found on device", Toast.LENGTH_LONG).show()
                    }
                } else {
                    OperationModeManager.setMode(this, selected)
                    updateModeButtonText()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDomainActionDialog(item: QueryLogItem) {
        val isWhitelisted = CustomRuleManager.isWhitelisted(item.domain)
        val isBlacklisted = CustomRuleManager.isBlacklisted(item.domain)

        val options = mutableListOf<String>()
        if (isWhitelisted) {
            options.add("Remove from Whitelist")
        } else {
            options.add("🟢 Always Allow / Whitelist")
        }

        if (isBlacklisted) {
            options.add("Remove from Blacklist")
        } else {
            options.add("🔴 Always Block / Blacklist")
        }

        options.add("📋 Copy Domain Name")

        AlertDialog.Builder(this)
            .setTitle(item.domain)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "🟢 Always Allow / Whitelist" -> {
                        CustomRuleManager.addWhitelist(this, item.domain)
                        Toast.makeText(this, "Whitelisted ${item.domain}", Toast.LENGTH_SHORT).show()
                    }
                    "Remove from Whitelist" -> {
                        CustomRuleManager.removeRule(this, item.domain)
                        Toast.makeText(this, "Removed from Whitelist", Toast.LENGTH_SHORT).show()
                    }
                    "🔴 Always Block / Blacklist" -> {
                        CustomRuleManager.addBlacklist(this, item.domain)
                        Toast.makeText(this, "Blacklisted ${item.domain}", Toast.LENGTH_SHORT).show()
                    }
                    "Remove from Blacklist" -> {
                        CustomRuleManager.removeRule(this, item.domain)
                        Toast.makeText(this, "Removed from Blacklist", Toast.LENGTH_SHORT).show()
                    }
                    "📋 Copy Domain Name" -> {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("domain", item.domain)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDnsSelectionDialog() {
        val providers = UpstreamDns.values()
        val names = providers.map { "${it.displayName}\n${it.description}" }.toTypedArray()
        val currentIndex = providers.indexOf(DnsProviderManager.currentProvider)

        AlertDialog.Builder(this)
            .setTitle("Select Upstream DNS Resolver")
            .setSingleChoiceItems(names, currentIndex) { dialog, which ->
                val selected = providers[which]
                dialog.dismiss()
                if (selected == UpstreamDns.CUSTOM) {
                    showCustomDnsPrompt()
                } else {
                    DnsProviderManager.setProvider(this, selected)
                    updateDnsButtonText()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCustomDnsPrompt() {
        val input = EditText(this)
        input.hint = "e.g. 45.90.28.0 or 192.168.1.100"
        input.setText(DnsProviderManager.customDnsIp)

        AlertDialog.Builder(this)
            .setTitle("Enter Custom DNS IP")
            .setMessage("Enter the IPv4 address of your private DNS, NextDNS, or local Pi-hole:")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val ip = input.text.toString().trim()
                if (ip.isNotEmpty()) {
                    DnsProviderManager.setProvider(this, UpstreamDns.CUSTOM, ip)
                    updateDnsButtonText()
                }
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
        if (OperationModeManager.currentMode == OperationMode.ROOT_HOSTS) {
            val currentlyActive = UltraVpnService.isRunning.get()
            val willEnable = !currentlyActive
            val success = OperationModeManager.applyRootHosts(this, willEnable)
            if (success) {
                UltraVpnService.isRunning.set(willEnable)
                updateUiState()
                Toast.makeText(this, if (willEnable) "Root hosts sinkhole active" else "Root hosts disabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to apply root hosts rules", Toast.LENGTH_LONG).show()
            }
            return
        }

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
