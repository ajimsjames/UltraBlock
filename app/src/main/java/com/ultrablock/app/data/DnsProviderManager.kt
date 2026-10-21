package com.ultrablock.app.data

import android.content.Context
import java.net.InetAddress

enum class UpstreamDns(val id: String, val displayName: String, val ip: String, val description: String) {
    CLOUDFLARE_STANDARD("cf_standard", "Cloudflare (1.1.1.1)", "1.1.1.1", "Fast, privacy-first DNS"),
    CLOUDFLARE_SECURITY("cf_security", "Cloudflare Security (1.1.1.2)", "1.1.1.2", "Malware & Phishing protection"),
    ADGUARD_DNS("adguard", "AdGuard DNS (94.140.14.14)", "94.140.14.14", "Upstream DNS ad & tracker blocker"),
    QUAD9_DNS("quad9", "Quad9 Privacy (9.9.9.9)", "9.9.9.9", "Swiss non-profit, malware blocking"),
    GOOGLE_DNS("google", "Google Public DNS (8.8.8.8)", "8.8.8.8", "Reliable global resolution")
}

object DnsProviderManager {
    private const val PREFS_NAME = "ultrablock_dns_prefs"
    private const val KEY_SELECTED_DNS = "selected_upstream_dns"

    @Volatile
    var currentProvider: UpstreamDns = UpstreamDns.CLOUDFLARE_STANDARD
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedId = prefs.getString(KEY_SELECTED_DNS, UpstreamDns.CLOUDFLARE_STANDARD.id)
        currentProvider = UpstreamDns.values().find { it.id == savedId } ?: UpstreamDns.CLOUDFLARE_STANDARD
    }

    fun setProvider(context: Context, provider: UpstreamDns) {
        currentProvider = provider
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED_DNS, provider.id).apply()
    }

    fun getUpstreamAddress(): InetAddress {
        return try {
            InetAddress.getByName(currentProvider.ip)
        } catch (_: Exception) {
            InetAddress.getByName("1.1.1.1")
        }
    }
}
