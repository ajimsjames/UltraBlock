package com.ultrablock.app.dns

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.LongBuffer

object DomainFilter {
    private const val TAG = "DomainFilter"
    private const val FNV_64_OFFSET = -0x340d631b7bdddcdbL
    private const val FNV_64_PRIME = 0x100000001b3L

    const val PREFS_NAME = "ultrablock_toggles"
    const val KEY_BLOCK_WPS = "block_wps"
    const val KEY_BLOCK_APP_TRACKERS = "block_app_trackers"
    const val KEY_BLOCK_INAPP_ADS = "block_inapp_ads"
    const val KEY_BLOCK_SMART_TV = "block_smart_tv"
    const val KEY_BLOCK_CHAVARA = "block_chavara"
    const val KEY_BLOCK_REALME = "block_realme"
    const val KEY_BLOCK_META = "block_meta"
    const val KEY_BLOCK_XIAOMI = "block_xiaomi"
    const val KEY_BLOCK_BYTEDANCE = "block_bytedance"

    @Volatile
    private var hashBuffer: LongBuffer? = null
    @Volatile
    var totalLoadedRules: Int = 0
        private set

    @Volatile
    var blockWps: Boolean = true
    @Volatile
    var blockAppTrackers: Boolean = true
    @Volatile
    var blockInAppAds: Boolean = true
    @Volatile
    var blockSmartTv: Boolean = true
    @Volatile
    var blockChavara: Boolean = false
    @Volatile
    var blockRealme: Boolean = true
    @Volatile
    var blockMeta: Boolean = true
    @Volatile
    var blockXiaomi: Boolean = true
    @Volatile
    var blockBytedance: Boolean = true

    val whitelistedApps = mutableSetOf<String>()

    @Synchronized
    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        blockWps = prefs.getBoolean(KEY_BLOCK_WPS, true)
        blockAppTrackers = prefs.getBoolean(KEY_BLOCK_APP_TRACKERS, true)
        blockInAppAds = prefs.getBoolean(KEY_BLOCK_INAPP_ADS, true)
        blockSmartTv = prefs.getBoolean(KEY_BLOCK_SMART_TV, true)
        blockChavara = prefs.getBoolean(KEY_BLOCK_CHAVARA, false)
        blockRealme = prefs.getBoolean(KEY_BLOCK_REALME, true)
        blockMeta = prefs.getBoolean(KEY_BLOCK_META, true)
        blockXiaomi = prefs.getBoolean(KEY_BLOCK_XIAOMI, true)
        blockBytedance = prefs.getBoolean(KEY_BLOCK_BYTEDANCE, true)

        val set = prefs.getStringSet("whitelisted_apps", null)
        synchronized(whitelistedApps) {
            whitelistedApps.clear()
            if (set != null) {
                whitelistedApps.addAll(set)
            }
        }

        if (hashBuffer != null) return
        try {
            val start = System.currentTimeMillis()
            val assetStream: InputStream = context.assets.open("hosts_ultra.bin")
            val bytes = assetStream.readBytes()
            assetStream.close()

            val byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val count = byteBuffer.int
            totalLoadedRules = count

            val longs = byteBuffer.asLongBuffer()
            hashBuffer = longs
            val duration = System.currentTimeMillis() - start
            Log.i(TAG, "Loaded $count domain hashes in ${duration}ms")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading domain filter", e)
        }
    }

    fun updateToggle(context: Context, key: String, enabled: Boolean) {
        when (key) {
            KEY_BLOCK_WPS -> blockWps = enabled
            KEY_BLOCK_APP_TRACKERS -> blockAppTrackers = enabled
            KEY_BLOCK_INAPP_ADS -> blockInAppAds = enabled
            KEY_BLOCK_SMART_TV -> blockSmartTv = enabled
            KEY_BLOCK_CHAVARA -> blockChavara = enabled
            KEY_BLOCK_REALME -> blockRealme = enabled
            KEY_BLOCK_META -> blockMeta = enabled
            KEY_BLOCK_XIAOMI -> blockXiaomi = enabled
            KEY_BLOCK_BYTEDANCE -> blockBytedance = enabled
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(key, enabled).apply()
    }

    fun isAppExempt(packageName: String?): Boolean {
        if (packageName == null) return false
        if (packageName == "appn.chavaramatrimony" && !blockChavara) return true
        synchronized(whitelistedApps) {
            return whitelistedApps.contains(packageName)
        }
    }

    fun setAppExempt(context: Context, packageName: String, exempt: Boolean) {
        synchronized(whitelistedApps) {
            if (exempt) {
                whitelistedApps.add(packageName)
            } else {
                whitelistedApps.remove(packageName)
            }
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putStringSet("whitelisted_apps", HashSet(whitelistedApps)).apply()
        }
    }

    fun hashDomain(domain: String): Long {
        var h = FNV_64_OFFSET
        val len = domain.length
        for (i in 0 until len) {
            val c = domain[i].lowercaseChar().code
            if (c < 128) {
                h = h xor c.toLong()
                h *= FNV_64_PRIME
            } else {
                val utf8Bytes = c.toChar().toString().toByteArray(Charsets.UTF_8)
                for (b in utf8Bytes) {
                    h = h xor (b.toInt() and 0xFF).toLong()
                    h *= FNV_64_PRIME
                }
            }
        }
        return h
    }

    fun isBlocked(domain: String, originatingPkg: String? = null): Boolean {
        if (isAppExempt(originatingPkg)) return false

        var currentDomain = domain.trim().trimEnd('.').lowercase()
        if (currentDomain.isEmpty()) return false

        if (!blockChavara && (currentDomain.contains("chavaramatrimony") || currentDomain.contains("chavara"))) {
            return false
        }

        // 1. Check custom toggle categories
        if (blockWps && matchesAny(currentDomain, CustomBlocklists.WPS_DOMAINS)) return true
        if (blockSmartTv && matchesAny(currentDomain, CustomBlocklists.SMART_TV_DOMAINS)) return true
        if (blockAppTrackers && matchesAny(currentDomain, CustomBlocklists.APP_TRACKERS_DOMAINS)) return true
        if (blockInAppAds && matchesAny(currentDomain, CustomBlocklists.INAPP_ADS_DOMAINS)) return true
        if (blockRealme && matchesAny(currentDomain, CustomBlocklists.REALME_OPPO_DOMAINS)) return true
        if (blockMeta && matchesAny(currentDomain, CustomBlocklists.META_DOMAINS)) return true
        if (blockXiaomi && matchesAny(currentDomain, CustomBlocklists.XIAOMI_DOMAINS)) return true
        if (blockBytedance && matchesAny(currentDomain, CustomBlocklists.BYTEDANCE_DOMAINS)) return true

        // 2. Check ultra hosts binary index
        val buffer = hashBuffer ?: return false
        val count = totalLoadedRules
        if (count == 0) return false

        var checkDomain = currentDomain
        while (checkDomain.isNotEmpty()) {
            val targetHash = hashDomain(checkDomain)
            if (binarySearch(buffer, count, targetHash)) {
                return true
            }
            val dotIndex = checkDomain.indexOf('.')
            if (dotIndex != -1 && dotIndex < checkDomain.length - 1) {
                checkDomain = checkDomain.substring(dotIndex + 1)
            } else {
                break
            }
        }
        return false
    }

    private fun matchesAny(domain: String, set: Set<String>): Boolean {
        if (set.contains(domain)) return true
        for (target in set) {
            if (domain.endsWith(".$target")) return true
        }
        return false
    }

    private fun binarySearch(buf: LongBuffer, size: Int, key: Long): Boolean {
        var low = 0
        var high = size - 1

        while (low <= high) {
            val mid = (low + high) ushr 1
            val midVal = buf.get(mid)

            val cmp = java.lang.Long.compareUnsigned(midVal, key)
            if (cmp < 0) {
                low = mid + 1
            } else if (cmp > 0) {
                high = mid - 1
            } else {
                return true
            }
        }
        return false
    }
}
