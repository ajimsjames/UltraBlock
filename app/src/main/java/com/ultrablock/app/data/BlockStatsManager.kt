package com.ultrablock.app.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.ultrablock.app.dns.DomainFilter
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class AppBlockItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val blockedCount: Int,
    val lastBlockedDomain: String,
    val lastTimestamp: Long,
    var isProtected: Boolean
)

object BlockStatsManager {
    private const val PREFS_NAME = "ultrablock_app_stats"
    private const val KEY_STATS = "stats_map"
    private const val KEY_DOMAINS = "last_domains"

    val appBlockedCounts = ConcurrentHashMap<String, AtomicInteger>()
    val appLastBlockedDomain = ConcurrentHashMap<String, String>()
    val appLastTimestamp = ConcurrentHashMap<String, Long>()

    private val DOMAIN_TO_APP_MAP = listOf(
        Regex(".*wps.*|.*kingsoft.*|.*ksosoft.*|.*kmoffice.*|.*4wps\\.net.*|.*docer.*") to "cn.wps.moffice_eng",
        Regex(".*flipkart.*|.*fkcdn.*") to "com.flipkart.android",
        Regex(".*zomato.*|.*zmtcdn.*") to "com.application.zomato",
        Regex(".*swiggy.*|.*swiggyops.*") to "in.swiggy.android",
        Regex(".*amazon-adsystem.*|.*aax.*amazon.*|.*fls-na\\.amazon.*") to "in.amazon.mShop.android.shopping",
        Regex(".*botim.*|.*thebot.*") to "im.thebot.messenger",
        Regex(".*chavaramatrimony.*|.*chavara.*") to "appn.chavaramatrimony",
        Regex(".*pocbible.*") to "com.pocbible.androidapp",
        Regex(".*sbi\\.co\\.in.*|.*onlinesbi.*") to "com.sbi.lotusintouch",
        Regex(".*federalbank\\.co\\.in.*|.*fedmobile.*") to "com.fedmobile",
        Regex(".*jiomart.*|.*relianceretail.*") to "com.jpl.jiomart",
        Regex(".*linkedin.*|.*licdn.*") to "com.linkedin.android",
        Regex(".*vungle.*") to "com.gamma.scan",
        Regex(".*heytap.*|.*oppo.*|.*coloros.*|.*realme.*|.*oplus.*|.*nearme.*") to "com.coloros.phonemanager",
        Regex(".*miui.*|.*xiaomi.*|.*mistat.*") to "com.miui.analytics"
    )

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_STATS, null)
        val domainJsonStr = prefs.getString(KEY_DOMAINS, null)

        if (jsonStr != null) {
            try {
                val json = JSONObject(jsonStr)
                json.keys().forEach { pkg ->
                    appBlockedCounts[pkg] = AtomicInteger(json.getInt(pkg))
                }
            } catch (ignored: Exception) {}
        }
        if (domainJsonStr != null) {
            try {
                val json = JSONObject(domainJsonStr)
                json.keys().forEach { pkg ->
                    appLastBlockedDomain[pkg] = json.getString(pkg)
                }
            } catch (ignored: Exception) {}
        }
    }

    fun recordBlock(context: Context, domain: String, attributedPkg: String?) {
        val targetPkg = attributedPkg ?: resolveAppFromDomain(domain) ?: "System & Background Ads"

        val counter = appBlockedCounts.computeIfAbsent(targetPkg) { AtomicInteger(0) }
        counter.incrementAndGet()
        appLastBlockedDomain[targetPkg] = domain
        appLastTimestamp[targetPkg] = System.currentTimeMillis()

        saveAsync(context)
    }

    fun resolveAppFromDomain(domain: String): String? {
        val lower = domain.lowercase()
        for ((regex, pkg) in DOMAIN_TO_APP_MAP) {
            if (regex.matches(lower)) return pkg
        }
        return null
    }

    private fun saveAsync(context: Context) {
        val json = JSONObject()
        appBlockedCounts.forEach { (pkg, count) ->
            json.put(pkg, count.get())
        }
        val domainJson = JSONObject()
        appLastBlockedDomain.forEach { (pkg, dom) ->
            domainJson.put(pkg, dom)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_STATS, json.toString())
            .putString(KEY_DOMAINS, domainJson.toString())
            .apply()
    }

    fun getAppItems(packageManager: PackageManager): List<AppBlockItem> {
        val list = mutableListOf<AppBlockItem>()
        val seenPkgs = HashSet<String>()

        // 1. Add all apps that had blocked activity
        appBlockedCounts.forEach { (pkg, count) ->
            seenPkgs.add(pkg)
            var appName = pkg
            var icon: Drawable? = null

            if (pkg == "System & Background Ads") {
                appName = "System & Third-Party Ad Networks"
            } else {
                try {
                    val appInfo = packageManager.getApplicationInfo(pkg, 0)
                    appName = packageManager.getApplicationLabel(appInfo).toString()
                    icon = packageManager.getApplicationIcon(appInfo)
                } catch (e: Exception) {
                    appName = pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
                }
            }

            val isExempt = DomainFilter.isAppExempt(pkg)

            list.add(
                AppBlockItem(
                    packageName = pkg,
                    appName = appName,
                    icon = icon,
                    blockedCount = count.get(),
                    lastBlockedDomain = appLastBlockedDomain[pkg] ?: "",
                    lastTimestamp = appLastTimestamp[pkg] ?: System.currentTimeMillis(),
                    isProtected = !isExempt
                )
            )
        }

        // 2. Explicitly include Chavara Matrimony and prominent installed apps so user can toggle them even before blocks occur
        val prominentPackages = listOf(
            "appn.chavaramatrimony",
            "cn.wps.moffice_eng",
            "com.flipkart.android",
            "in.swiggy.android",
            "com.application.zomato",
            "com.jpl.jiomart",
            "im.thebot.messenger",
            "com.gamma.scan"
        )

        for (pkg in prominentPackages) {
            if (!seenPkgs.contains(pkg)) {
                try {
                    val appInfo = packageManager.getApplicationInfo(pkg, 0)
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val icon = packageManager.getApplicationIcon(appInfo)
                    val isExempt = DomainFilter.isAppExempt(pkg)

                    list.add(
                        AppBlockItem(
                            packageName = pkg,
                            appName = appName,
                            icon = icon,
                            blockedCount = 0,
                            lastBlockedDomain = "",
                            lastTimestamp = 0L,
                            isProtected = !isExempt
                        )
                    )
                    seenPkgs.add(pkg)
                } catch (ignored: Exception) {}
            }
        }

        return list.sortedWith(
            compareByDescending<AppBlockItem> { it.blockedCount }
                .thenBy { it.appName.lowercase() }
        )
    }

    fun resetStats(context: Context) {
        appBlockedCounts.clear()
        appLastBlockedDomain.clear()
        appLastTimestamp.clear()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
