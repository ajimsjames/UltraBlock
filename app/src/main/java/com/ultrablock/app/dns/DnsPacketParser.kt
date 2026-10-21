package com.ultrablock.app.dns

import java.nio.ByteBuffer

object DnsPacketParser {

    data class DnsQuestion(
        val domain: String,
        val qType: Int,
        val qClass: Int
    )

    fun parseQuestion(packet: ByteArray, offset: Int, length: Int): Pair<DnsQuestion, Int>? {
        if (length < 12) return null
        val buffer = ByteBuffer.wrap(packet, offset, length)

        val id = buffer.short
        val flags = buffer.short.toInt() and 0xFFFF
        val isQuery = (flags and 0x8000) == 0
        val qdCount = buffer.short.toInt() and 0xFFFF

        if (!isQuery || qdCount < 1) return null

        val anCount = buffer.short
        val nsCount = buffer.short
        val arCount = buffer.short

        val domainBuilder = java.lang.StringBuilder()
        var pos = 12

        while (pos < length) {
            val labelLen = packet[offset + pos].toInt() and 0xFF
            pos++
            if (labelLen == 0) break
            if (pos + labelLen > length) return null

            if (domainBuilder.isNotEmpty()) {
                domainBuilder.append('.')
            }
            for (i in 0 until labelLen) {
                domainBuilder.append(packet[offset + pos + i].toInt().toChar())
            }
            pos += labelLen
        }

        if (pos + 4 > length) return null
        val qType = ByteBuffer.wrap(packet, offset + pos, 2).short.toInt() and 0xFFFF
        val qClass = ByteBuffer.wrap(packet, offset + pos + 2, 2).short.toInt() and 0xFFFF
        pos += 4

        return Pair(DnsQuestion(domainBuilder.toString(), qType, qClass), pos)
    }

    /**
     * Builds a standard DNS response returning 0.0.0.0 (A record) or :: (AAAA record)
     */
    fun buildSinkholeResponse(queryPacket: ByteArray, queryLen: Int, questionEndPos: Int, qType: Int): ByteArray {
        val out = ByteBuffer.allocate(512)

        // Transaction ID from query
        out.put(queryPacket, 0, 2)

        // Flags: Standard response, No error, Authoritative
        // 0x8180 = QR=1, Opcode=0, AA=1, RD=1, RA=1, Rcode=0
        out.putShort(0x8180.toShort())

        // QDCOUNT = 1
        out.putShort(1)
        // ANCOUNT = 1
        out.putShort(1)
        // NSCOUNT = 0
        out.putShort(0)
        // ARCOUNT = 0
        out.putShort(0)

        // Question section copied verbatim from query
        out.put(queryPacket, 12, questionEndPos - 12)

        // Answer Record:
        // Name pointer to question domain (offset 12 -> 0xC00C)
        out.putShort(0xC00C.toShort())

        if (qType == 28) {
            // AAAA record (IPv6 sinkhole -> ::)
            out.putShort(28.toShort()) // Type AAAA
            out.putShort(1.toShort())  // Class IN
            out.putInt(300)            // TTL 300 seconds
            out.putShort(16.toShort()) // Data length 16 bytes
            out.put(ByteArray(16))     // 0000:0000:...
        } else {
            // A record (IPv4 sinkhole -> 0.0.0.0)
            out.putShort(1.toShort())  // Type A
            out.putShort(1.toShort())  // Class IN
            out.putInt(300)            // TTL 300 seconds
            out.putShort(4.toShort())  // Data length 4 bytes
            out.put(byteArrayOf(0, 0, 0, 0)) // 0.0.0.0
        }

        val len = out.position()
        val result = ByteArray(len)
        System.arraycopy(out.array(), 0, result, 0, len)
        return result
    }
}
