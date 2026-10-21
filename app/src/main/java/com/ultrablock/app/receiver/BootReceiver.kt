package com.ultrablock.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.content.ContextCompat
import com.ultrablock.app.service.UltraVpnService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val vpnIntent = VpnService.prepare(context)
            if (vpnIntent == null) {
                val serviceIntent = Intent(context, UltraVpnService::class.java).apply {
                    action = UltraVpnService.ACTION_START
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }
}
