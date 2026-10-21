package com.ultrablock.app.data

import android.content.Context
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

object CustomRuleManager {
    private const val PREFS_NAME = "ultrablock_custom_rules"
    private const val KEY_WHITELIST = "custom_whitelist"
    private const val KEY_BLACKLIST = "custom_blacklist"

    val customWhitelist: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())
    val customBlacklist: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val wl = prefs.getStringSet(KEY_WHITELIST, null)
        val bl = prefs.getStringSet(KEY_BLACKLIST, null)

        customWhitelist.clear()
        if (wl != null) customWhitelist.addAll(wl.map { it.trim().lowercase() })

        customBlacklist.clear()
        if (bl != null) customBlacklist.addAll(bl.map { it.trim().lowercase() })
    }

    fun addWhitelist(context: Context, domain: String) {
        val clean = domain.trim().trimEnd('.').lowercase()
        if (clean.isEmpty()) return
        customBlacklist.remove(clean)
        customWhitelist.add(clean)
        persist(context)
    }

    fun addBlacklist(context: Context, domain: String) {
        val clean = domain.trim().trimEnd('.').lowercase()
        if (clean.isEmpty()) return
        customWhitelist.remove(clean)
        customBlacklist.add(clean)
        persist(context)
    }

    fun removeRule(context: Context, domain: String) {
        val clean = domain.trim().trimEnd('.').lowercase()
        customWhitelist.remove(clean)
        customBlacklist.remove(clean)
        persist(context)
    }

    fun isWhitelisted(domain: String): Boolean {
        if (customWhitelist.contains(domain)) return true
        for (w in customWhitelist) {
            if (domain.endsWith(".$w")) return true
        }
        return false
    }

    fun isBlacklisted(domain: String): Boolean {
        if (customBlacklist.contains(domain)) return true
        for (b in customBlacklist) {
            if (domain.endsWith(".$b")) return true
        }
        return false
    }

    private fun persist(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_WHITELIST, customWhitelist)
            .putStringSet(KEY_BLACKLIST, customBlacklist)
            .apply()
    }
}
