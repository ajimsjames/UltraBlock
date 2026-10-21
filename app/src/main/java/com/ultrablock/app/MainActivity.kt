package com.ultrablock.app

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.VpnService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import com.ultrablock.app.data.BlockStatsManager
import com.ultrablock.app.dns.DomainFilter
import com.ultrablock.app.service.UltraVpnService
import com.ultrablock.app.ui.AppStatsActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnToggleShield: FrameLayout
    private lateinit var imgShieldIcon: ImageView
    private lateinit var txtStatusTitle: TextView
    private lateinit var txtStatusSubtitle: TextView
    private lateinit var txtBlockedCount: TextView
    private lateinit var txtProcessedCount: TextView
    private lateinit var txtLastBlocked: TextView
    private lateinit var btnOpenAppStats: Button

    private lateinit var switchBlockSmartTv: SwitchMaterial
    private lateinit var switchBlockInAppAds: SwitchMaterial
    private lateinit var switchBlockAppTrackers: SwitchMaterial
    private lateinit var switchBlockWps: SwitchMaterial
    private lateinit var switchBlockRealme: SwitchMaterial
    private lateinit var switchBlockMeta: SwitchMaterial
    private lateinit var switchBlockXiaomi: SwitchMaterial
    private lateinit var switchBlockBytedance: SwitchMaterial

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

        btnToggleShield = findViewById(R.id.btnToggleShield)
        imgShieldIcon = findViewById(R.id.imgShieldIcon)
        txtStatusTitle = findViewById(R.id.txtStatusTitle)
        txtStatusSubtitle = findViewById(R.id.txtStatusSubtitle)
        txtBlockedCount = findViewById(R.id.txtBlockedCount)
        txtProcessedCount = findViewById(R.id.txtProcessedCount)
        txtLastBlocked = findViewById(R.id.txtLastBlocked)
        btnOpenAppStats = findViewById(R.id.btnOpenAppStats)

        switchBlockSmartTv = findViewById(R.id.switchBlockSmartTv)
        switchBlockInAppAds = findViewById(R.id.switchBlockInAppAds)
        switchBlockAppTrackers = findViewById(R.id.switchBlockAppTrackers)
        switchBlockWps = findViewById(R.id.switchBlockWps)
        switchBlockRealme = findViewById(R.id.switchBlockRealme)
        switchBlockMeta = findViewById(R.id.switchBlockMeta)
        switchBlockXiaomi = findViewById(R.id.switchBlockXiaomi)
        switchBlockBytedance = findViewById(R.id.switchBlockBytedance)

        btnToggleShield.setOnClickListener {
            toggleShield()
        }

        btnOpenAppStats.setOnClickListener {
            val intent = Intent(this, AppStatsActivity::class.java)
            startActivity(intent)
        }

        setupCategoryToggles()
    }

    private fun setupCategoryToggles() {
        switchBlockSmartTv.isChecked = DomainFilter.blockSmartTv
        switchBlockInAppAds.isChecked = DomainFilter.blockInAppAds
        switchBlockAppTrackers.isChecked = DomainFilter.blockAppTrackers
        switchBlockWps.isChecked = DomainFilter.blockWps
        switchBlockRealme.isChecked = DomainFilter.blockRealme
        switchBlockMeta.isChecked = DomainFilter.blockMeta
        switchBlockXiaomi.isChecked = DomainFilter.blockXiaomi
        switchBlockBytedance.isChecked = DomainFilter.blockBytedance

        switchBlockSmartTv.setOnCheckedChangeListener { _, isChecked ->
            DomainFilter.updateToggle(this, DomainFilter.KEY_BLOCK_SMART_TV, isChecked)
        }
        switchBlockInAppAds.setOnCheckedChangeListener { _, isChecked ->
            DomainFilter.updateToggle(this, DomainFilter.KEY_BLOCK_INAPP_ADS, isChecked)
        }
        switchBlockAppTrackers.setOnCheckedChangeListener { _, isChecked ->
            DomainFilter.updateToggle(this, DomainFilter.KEY_BLOCK_APP_TRACKERS, isChecked)
        }
        switchBlockWps.setOnCheckedChangeListener { _, isChecked ->
            DomainFilter.updateToggle(this, DomainFilter.KEY_BLOCK_WPS, isChecked)
        }
        switchBlockRealme.setOnCheckedChangeListener { _, isChecked ->
            DomainFilter.updateToggle(this, DomainFilter.KEY_BLOCK_REALME, isChecked)
        }
        switchBlockMeta.setOnCheckedChangeListener { _, isChecked ->
            DomainFilter.updateToggle(this, DomainFilter.KEY_BLOCK_META, isChecked)
        }
        switchBlockXiaomi.setOnCheckedChangeListener { _, isChecked ->
            DomainFilter.updateToggle(this, DomainFilter.KEY_BLOCK_XIAOMI, isChecked)
        }
        switchBlockBytedance.setOnCheckedChangeListener { _, isChecked ->
            DomainFilter.updateToggle(this, DomainFilter.KEY_BLOCK_BYTEDANCE, isChecked)
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
        val green = Color.parseColor("#10B981")
        val slate = Color.parseColor("#94A3B8")

        if (active) {
            txtStatusTitle.text = getString(R.string.shield_on)
            txtStatusTitle.setTextColor(green)
            imgShieldIcon.imageTintList = ColorStateList.valueOf(green)
        } else {
            txtStatusTitle.text = getString(R.string.shield_off)
            txtStatusTitle.setTextColor(Color.WHITE)
            imgShieldIcon.imageTintList = ColorStateList.valueOf(slate)
        }
        updateStats()
    }

    private fun updateStats() {
        txtBlockedCount.text = UltraVpnService.adsBlocked.get().toString()
        txtProcessedCount.text = UltraVpnService.queriesProcessed.get().toString()

        val last = UltraVpnService.lastBlockedDomain
        if (last != null) {
            txtLastBlocked.text = "Last sinkholed: $last"
        }
    }
}
