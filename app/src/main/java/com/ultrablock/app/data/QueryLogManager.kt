package com.ultrablock.app.data

import java.util.concurrent.ConcurrentLinkedDeque

data class QueryLogItem(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val domain: String,
    val packageName: String?,
    val isBlocked: Boolean,
    val reason: String = if (isBlocked) "Blocked Tracker / Telemetry" else "Resolved"
)

object QueryLogManager {
    private const val MAX_LOGS = 150
    private val logs = ConcurrentLinkedDeque<QueryLogItem>()

    fun recordQuery(domain: String, packageName: String?, isBlocked: Boolean, reason: String = if (isBlocked) "Blocked Tracker" else "Allowed") {
        logs.addFirst(QueryLogItem(
            domain = domain,
            packageName = packageName,
            isBlocked = isBlocked,
            reason = reason
        ))
        while (logs.size > MAX_LOGS) {
            logs.removeLast()
        }
    }

    fun getLogs(): List<QueryLogItem> {
        return logs.toList()
    }

    fun clear() {
        logs.clear()
    }
}
