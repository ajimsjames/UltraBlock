package com.ultrablock.app.data

import android.content.Context
import java.io.DataOutputStream

enum class OperationMode(val id: String, val title: String, val desc: String) {
    VPN("vpn", "Local VPN Mode (Default)", "Works on unrooted devices without modifying system partitions."),
    ROOT_HOSTS("root_hosts", "Root /etc/hosts Mode", "Direct system-level sinkhole via KernelSU/Magisk root without active VPN indicator.")
}

object OperationModeManager {
    private const val PREFS_NAME = "ultrablock_mode_prefs"
    private const val KEY_MODE = "operation_mode"

    @Volatile
    var currentMode: OperationMode = OperationMode.VPN
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modeStr = prefs.getString(KEY_MODE, OperationMode.VPN.id)
        currentMode = OperationMode.values().find { it.id == modeStr } ?: OperationMode.VPN
    }

    fun setMode(context: Context, mode: OperationMode) {
        currentMode = mode
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.id)
            .apply()
    }

    fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val exit = process.waitFor()
            exit == 0
        } catch (_: Exception) {
            false
        }
    }

    fun applyRootHosts(context: Context, enable: Boolean): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            if (enable) {
                // Mount /system as RW or use systemless hosts overlay
                os.writeBytes("magisk --path >/dev/null 2>&1 && magisk --install-module /data/adb/modules/hosts 2>/dev/null\n")
                os.writeBytes("cp /data/data/com.ultrablock.app/files/hosts_ultra.txt /system/etc/hosts 2>/dev/null || cp /data/data/com.ultrablock.app/files/hosts_ultra.txt /etc/hosts 2>/dev/null\n")
            } else {
                os.writeBytes("echo '127.0.0.1 localhost\n::1 localhost' > /etc/hosts\n")
            }
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }
}
