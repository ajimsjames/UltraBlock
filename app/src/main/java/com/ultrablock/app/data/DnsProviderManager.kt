package com.ultrablock.app.data

import android.content.Context
import java.net.InetAddress

enum class UpstreamDns(val id: String, val displayName: String, val ip: String, val description: String) {
    CLOUDFLARE_STANDARD("cf_standard", "Cloudflare (1.1.1.1)", "1.1.1.1", "Fast, privacy-first DNS"),
    CLOUDFLARE_SECURITY("cf_security", "Cloudflare Security (1.1.1.2)", "1.1.1.2", "Malware & Phishing protection"),
    ADGUARD_DNS("adguard", "AdGuard DNS (94.140.14.14)", "94.140.14.14", "Upstream DNS ad & tracker blocker"),
    QUAD9_DNS("quad9", "Quad9 Privacy (9.9.9.9)", "9.9.9.9", "Swiss non-profit, malware blocking"),
    GOOGLE_DNS("google", "Google Public DNS (8.8.8.8)", "8.8.8.8", "Reliable global resolution"),
    CUSTOM("custom", "Custom DNS / NextDNS / Pi-hole", "", "User-defined upstream resolver IP")
}

object DnsProviderManager {
    private const val PREFS_NAME = "ultrablock_dns_prefs"
    private const val KEY_SELECTED_DNS = "selected_upstream_dns"
    private const val KEY_CUSTOM_IP = "custom_dns_ip"

    @Volatile
    var currentProvider: UpstreamDns = UpstreamDns.CLOUDFLARE_STANDARD
        private set

    @Volatile
    var customDnsIp: String = "1.1.1.1"
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedId = prefs.getString(KEY_SELECTED_DNS, UpstreamDns.CLOUDFLARE_STANDARD.id)
        customDnsIp = prefs.getString(KEY_CUSTOM_IP, "1.1.1.1") ?: "1.1.1.1"
        currentProvider = UpstreamDns.values().find { it.id == savedId } ?: UpstreamDns.CLOUDFLARE_STANDARD
    }

    fun setProvider(context: Context, provider: UpstreamDns, customIp: String? = null) {
        currentProvider = provider
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit().putString(KEY_SELECTED_DNS, provider.id)
        if (customIp != null && customIp.isNotBlank()) {
            customDnsIp = customIp.trim()
            editor.putString(KEY_CUSTOM_IP, customDnsIp)
        }
        editor.apply()
    }

    fun getUpstreamAddress(): InetAddress {
        val ipToUse = if (currentProvider == UpstreamDns.CUSTOM) customDnsIp else currentProvider.ip
        return try {
            InetAddress.getByName(ipToUse)
        } catch (_: Exception) {
            InetAddress.getByName("1.1.1.1")
        }
    }
}
