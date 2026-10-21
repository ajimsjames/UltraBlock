package com.ultrablock.app.data

import android.content.Context
import android.os.SystemClock

object SnoozeManager {
    private const val PREFS_NAME = "ultrablock_snooze"
    private const val KEY_SNOOZE_UNTIL = "snooze_until_walltime"

    @Volatile
    private var snoozeUntilWallTime: Long = 0L

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        snoozeUntilWallTime = prefs.getLong(KEY_SNOOZE_UNTIL, 0L)
    }

    fun isSnoozed(): Boolean {
        val now = System.currentTimeMillis()
        return snoozeUntilWallTime > now
    }

    fun getRemainingMinutes(): Int {
        val diff = snoozeUntilWallTime - System.currentTimeMillis()
        return if (diff > 0) ((diff / 1000) / 60).toInt() + 1 else 0
    }

    fun snooze(context: Context, minutes: Int) {
        val until = System.currentTimeMillis() + (minutes * 60 * 1000L)
        snoozeUntilWallTime = until
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_SNOOZE_UNTIL, until)
            .apply()
    }

    fun cancelSnooze(context: Context) {
        snoozeUntilWallTime = 0L
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SNOOZE_UNTIL)
            .apply()
    }
}
