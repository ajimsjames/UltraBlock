package com.ultrablock.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ultrablock.app.MainActivity
import com.ultrablock.app.data.BlockStatsManager
import com.ultrablock.app.dns.DnsPacketParser
import com.ultrablock.app.dns.DomainFilter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class UltraVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.ultrablock.app.START"
        const val ACTION_STOP = "com.ultrablock.app.STOP"
        const val ACTION_STATE_CHANGED = "com.ultrablock.app.STATE_CHANGED"
        const val CHANNEL_ID = "ultrablock_channel"

        val isRunning = AtomicBoolean(false)
        val queriesProcessed = AtomicInteger(0)
        val adsBlocked = AtomicInteger(0)
        var lastBlockedDomain: String? = null
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnThread: Thread? = null
    private val shouldRun = AtomicBoolean(false)
    private var connectivityManager: ConnectivityManager? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        DomainFilter.load(applicationContext)
        BlockStatsManager.init(applicationContext)
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopVpn()
            else -> startVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (isRunning.get()) return

        val notification = createNotification()
        startForeground(1, notification)

        try {
            val builder = Builder()
                .setSession("UltraBlock")
                .addAddress("10.200.0.1", 32)
                .addDnsServer("10.200.0.2")
                .addRoute("10.200.0.2", 32)
                .setMtu(1500)
                .setBlocking(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            try {
                builder.addDisallowedApplication(packageName)
            } catch (ignored: Exception) {}

            vpnInterface = builder.establish()
            if (vpnInterface == null) {
                stopSelf()
                return
            }

            shouldRun.set(true)
            isRunning.set(true)
            notifyStateChanged()

            vpnThread = Thread({ runDnsLoop() }, "UltraBlock-DnsWorker").apply { start() }

        } catch (e: Exception) {
            Log.e("UltraVpnService", "Failed to start VPN", e)
            stopVpn()
        }
    }

    private fun stopVpn() {
        shouldRun.set(false)
        isRunning.set(false)
        notifyStateChanged()

        try {
            vpnInterface?.close()
        } catch (ignored: Exception) {}
        vpnInterface = null

        vpnThread?.interrupt()
        vpnThread = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun runDnsLoop() {
        val pfd = vpnInterface ?: return
        val inputStream = FileInputStream(pfd.fileDescriptor)
        val outputStream = FileOutputStream(pfd.fileDescriptor)

        val packet = ByteArray(32767)
        var upstreamSocket: DatagramSocket? = null

        try {
            upstreamSocket = DatagramSocket()
            protect(upstreamSocket)
            upstreamSocket.soTimeout = 4000
        } catch (e: Exception) {
            Log.e("UltraVpnService", "Error creating upstream socket", e)
        }

        val upstreamDns = InetAddress.getByName("1.1.1.1")

        while (shouldRun.get()) {
            try {
                val length = inputStream.read(packet)
                if (length <= 0) continue

                val version = (packet[0].toInt() ushr 4) and 0x0F
                if (version != 4) continue

                val ihl = (packet[0].toInt() and 0x0F) * 4
                val protocol = packet[9].toInt() and 0xFF

                if (protocol != 17) continue
                val udpHeaderOffset = ihl
                val udpPayloadOffset = udpHeaderOffset + 8
                val udpPayloadLen = length - udpPayloadOffset

                if (udpPayloadLen <= 12) continue

                val parseResult = DnsPacketParser.parseQuestion(packet, udpPayloadOffset, udpPayloadLen)
                if (parseResult != null) {
                    val (question, endPos) = parseResult
                    queriesProcessed.incrementAndGet()

                    val originatingPkg = getOriginatingPackage(packet, ihl)
                    val isAdOrTracker = DomainFilter.isBlocked(question.domain, originatingPkg)

                    val dnsResponseBytes: ByteArray? = if (isAdOrTracker) {
                        adsBlocked.incrementAndGet()
                        lastBlockedDomain = question.domain

                        // Query UID / Package of originating socket
                        BlockStatsManager.recordBlock(applicationContext, question.domain, originatingPkg)

                        DnsPacketParser.buildSinkholeResponse(packet.copyOfRange(udpPayloadOffset, length), udpPayloadLen, endPos, question.qType)
                    } else {
                        forwardQuery(upstreamSocket, packet, udpPayloadOffset, udpPayloadLen, upstreamDns)
                    }

                    if (dnsResponseBytes != null) {
                        val replyPacket = buildIpUdpReply(packet, ihl, dnsResponseBytes)
                        synchronized(outputStream) {
                            outputStream.write(replyPacket)
                        }
                    }
                }
            } catch (e: Exception) {
                if (!shouldRun.get()) break
            }
        }

        try {
            upstreamSocket?.close()
        } catch (ignored: Exception) {}
    }

    private fun getOriginatingPackage(packet: ByteArray, ihl: Int): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val cm = connectivityManager ?: return null
                // Parse source IP and source Port
                val srcIp = InetAddress.getByAddress(packet.copyOfRange(12, 16))
                val srcPort = ByteBuffer.wrap(packet, ihl, 2).short.toInt() and 0xFFFF
                val dstIp = InetAddress.getByAddress(packet.copyOfRange(16, 20))
                val dstPort = ByteBuffer.wrap(packet, ihl + 2, 2).short.toInt() and 0xFFFF

                val srcSocket = InetSocketAddress(srcIp, srcPort)
                val dstSocket = InetSocketAddress(dstIp, dstPort)

                val uid = cm.getConnectionOwnerUid(17, srcSocket, dstSocket)
                if (uid > 0) {
                    val names = packageManager.getPackagesForUid(uid)
                    if (!names.isNullOrEmpty()) {
                        return names[0]
                    }
                }
            } catch (ignored: Exception) {}
        }
        return null
    }

    private fun forwardQuery(socket: DatagramSocket?, packet: ByteArray, offset: Int, length: Int, upstreamDns: InetAddress): ByteArray? {
        if (socket == null) return null
        return try {
            val queryData = ByteArray(length)
            System.arraycopy(packet, offset, queryData, 0, length)
            val sendPacket = DatagramPacket(queryData, length, upstreamDns, 53)
            socket.send(sendPacket)

            val recvBuf = ByteArray(4096)
            val recvPacket = DatagramPacket(recvBuf, recvBuf.size)
            socket.receive(recvPacket)

            val resp = ByteArray(recvPacket.length)
            System.arraycopy(recvBuf, 0, resp, 0, recvPacket.length)
            resp
        } catch (e: Exception) {
            null
        }
    }

    private fun buildIpUdpReply(origPacket: ByteArray, origIhl: Int, dnsResponse: ByteArray): ByteArray {
        val totalLen = 20 + 8 + dnsResponse.size
        val out = ByteArray(totalLen)
        val buf = ByteBuffer.wrap(out)

        buf.put(0x45.toByte())
        buf.put(0.toByte())
        buf.putShort(totalLen.toShort())
        buf.putShort(0.toShort())
        buf.putShort(0x4000.toShort())
        buf.put(64.toByte())
        buf.put(17.toByte())
        buf.putShort(0.toShort())

        buf.put(origPacket, 16, 4)
        buf.put(origPacket, 12, 4)

        var sum = 0
        for (i in 0 until 10) {
            sum += ByteBuffer.wrap(out, i * 2, 2).short.toInt() and 0xFFFF
        }
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        val ipChecksum = (sum.inv() and 0xFFFF).toShort()
        buf.putShort(10, ipChecksum)

        buf.put(origPacket, origIhl + 2, 2)
        buf.put(origPacket, origIhl, 2)
        val udpLen = 8 + dnsResponse.size
        buf.putShort(udpLen.toShort())
        buf.putShort(0.toShort())

        buf.put(dnsResponse)

        return out
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "UltraBlock Shield",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active shield status"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("UltraBlock Active")
            .setContentText("Adblock & Telemetry Firewall: Active")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun notifyStateChanged() {
        sendBroadcast(Intent(ACTION_STATE_CHANGED).setPackage(packageName))
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
