package com.ultrablock.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.ultrablock.app.MainActivity

class UltraTileService : TileService() {

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateTileState()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        val filter = IntentFilter(UltraVpnService.ACTION_STATE_CHANGED)
        ContextCompat.registerReceiver(
            this,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        updateTileState()
    }

    override fun onStopListening() {
        super.onStopListening()
        try {
            unregisterReceiver(receiver)
        } catch (ignored: Exception) {}
    }

    override fun onClick() {
        super.onClick()
        val running = UltraVpnService.isRunning.get()

        if (running) {
            val stopIntent = Intent(this, UltraVpnService::class.java).apply {
                action = UltraVpnService.ACTION_STOP
            }
            startService(stopIntent)
        } else {
            // Check if VPN permission is granted
            val prepareIntent = VpnService.prepare(this)
            if (prepareIntent != null) {
                // Needs user permission confirmation -> open MainActivity
                val appIntent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivityAndCollapse(appIntent)
            } else {
                val startIntent = Intent(this, UltraVpnService::class.java).apply {
                    action = UltraVpnService.ACTION_START
                }
                ContextCompat.startForegroundService(this, startIntent)
            }
        }
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val active = UltraVpnService.isRunning.get()

        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "UltraBlock"
        tile.subtitle = if (active) "Active (563K rules)" else "Paused"
        tile.updateTile()
    }
}
