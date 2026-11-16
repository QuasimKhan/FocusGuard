package com.focusguard.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DnsVpnService : VpnService() {

    companion object {
        const val ACTION_STOP = "com.focusguard.app.STOP_VPN"
        private const val CHANNEL_ID = "focusguard_vpn_channel"
        private const val NOTIF_ID = 101
        private const val UPSTREAM_DNS = "1.1.1.1"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var workerThread: Thread? = null
    private var pool: ExecutorService? = null

    @Volatile
    private var running = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (running) return
        startForegroundNotification()
        DomainBlocklist.ensureLoaded(this)

        val builder = Builder()
            .setSession("FocusGuard DNS Shield")
            // Capture IPv4 DNS
            .addAddress("10.0.0.2", 32)
            .addDnsServer("10.0.0.1")
            .addRoute("10.0.0.1", 32)
            // Capture IPv6 DNS (prevents carrier IPv6 DNS leaks on 4G/5G/Wi-Fi)
            .addAddress("fd00:1:fd00::2", 128)
            .addDnsServer("fd00:1:fd00::1")
            .addRoute("fd00:1:fd00::1", 128)
            // Blackhole hardcoded DoH/DoT resolver IPs so Chrome falls back to local DNS
            .addRoute("8.8.8.8", 32)
            .addRoute("8.8.4.4", 32)
            .addRoute("1.0.0.1", 32)
            .addRoute("9.9.9.9", 32)
            .addRoute("149.112.112.112", 32)
            .addRoute("208.67.222.222", 32)
            .addRoute("208.67.220.220", 32)
            .addRoute("94.140.14.14", 32)
            .addRoute("94.140.15.15", 32)
            .addRoute("2001:4860:4860::8888", 128)
            .addRoute("2001:4860:4860::8844", 128)
            .addRoute("2606:4700:4700::1001", 128)
            .addRoute("2620:fe::fe", 128)
            .addRoute("2620:fe::9", 128)
            .setMtu(1280)
            .setBlocking(true)

        try {
            builder.addDisallowedApplication(packageName)
        } catch (_: Exception) {
        }

        vpnInterface = builder.establish() ?: return
        FocusPrefs.setVpnEnabled(this, true)
        running = true
        pool = Executors.newCachedThreadPool()

        workerThread = Thread {
            runPacketLoop()
        }.apply {
            name = "FocusGuardDnsLoop"
            start()
        }
    }

    private fun runPacketLoop() {
        val pfd = vpnInterface ?: return
        val input = FileInputStream(pfd.fileDescriptor)
        val output = FileOutputStream(pfd.fileDescriptor)
        val buffer = ByteArray(2048)

        while (running) {
            val length = try {
                input.read(buffer)
            } catch (_: Exception) {
                break
            }
            if (length <= 28) continue

            val version = (buffer[0].toInt() ushr 4) and 0x0F
            if (version == 4) {
                handleIpv4Packet(buffer, length, output)
            } else if (version == 6) {
                handleIpv6Packet(buffer, length, output)
            }
        }
    }

    private fun handleIpv4Packet(buffer: ByteArray, length: Int, output: FileOutputStream) {
        val ihl = (buffer[0].toInt() and 0x0F) * 4
        if (length < ihl + 8) return

        val protocol = buffer[9].toInt() and 0xFF
        if (protocol != 17) return // Drop TCP/non-UDP (blocks DoH over HTTPS to 8.8.8.8/1.0.0.1)

        val srcPort = ((buffer[ihl].toInt() and 0xFF) shl 8) or (buffer[ihl + 1].toInt() and 0xFF)
        val dstPort = ((buffer[ihl + 2].toInt() and 0xFF) shl 8) or (buffer[ihl + 3].toInt() and 0xFF)
        if (dstPort != 53) return // Drop UDP port 443 QUIC DoH probes

        val dnsOffset = ihl + 8
        val dnsLength = length - dnsOffset
        if (dnsLength < 12) return

        val packetCopy = buffer.copyOf(length)
        val domain = parseQueryDomain(packetCopy, dnsOffset, length)

        if (domain != null && DomainBlocklist.isBlocked(this, domain)) {
            val nxDomainDns = buildNxDomainPayload(packetCopy, dnsOffset, length)
            val responsePacket = buildIpv4UdpResponse(packetCopy, srcPort, dstPort, nxDomainDns)
            synchronized(output) {
                try {
                    output.write(responsePacket)
                } catch (_: Exception) {
                }
            }
        } else {
            pool?.execute {
                forwardUpstreamIpv4(packetCopy, srcPort, dstPort, dnsOffset, dnsLength, output)
            }
        }
    }

    private fun handleIpv6Packet(buffer: ByteArray, length: Int, output: FileOutputStream) {
        if (length < 48) return
        val nextHeader = buffer[6].toInt() and 0xFF
        if (nextHeader != 17) return // UDP only

        val srcPort = ((buffer[40].toInt() and 0xFF) shl 8) or (buffer[41].toInt() and 0xFF)
        val dstPort = ((buffer[42].toInt() and 0xFF) shl 8) or (buffer[43].toInt() and 0xFF)
        if (dstPort != 53) return

        val dnsOffset = 48
        val dnsLength = length - dnsOffset
        if (dnsLength < 12) return

        val packetCopy = buffer.copyOf(length)
        val domain = parseQueryDomain(packetCopy, dnsOffset, length)

        if (domain != null && DomainBlocklist.isBlocked(this, domain)) {
            val nxDomainDns = buildNxDomainPayload(packetCopy, dnsOffset, length)
            val responsePacket = buildIpv6UdpResponse(packetCopy, srcPort, dstPort, nxDomainDns)
            synchronized(output) {
                try {
                    output.write(responsePacket)
                } catch (_: Exception) {
                }
            }
        } else {
            pool?.execute {
                forwardUpstreamIpv6(packetCopy, srcPort, dstPort, dnsOffset, dnsLength, output)
            }
        }
    }

    private fun parseQueryDomain(packet: ByteArray, dnsOffset: Int, packetLen: Int): String? {
        var pos = dnsOffset + 12
        val sb = StringBuilder()

        while (pos < packetLen) {
            val len = packet[pos].toInt() and 0xFF
            if (len == 0) break
            if ((len and 0xC0) != 0) return null
            pos++
            if (pos + len > packetLen) return null
            if (sb.isNotEmpty()) sb.append('.')
            for (i in 0 until len) {
                sb.append((packet[pos + i].toInt() and 0xFF).toChar())
            }
            pos += len
        }
        return if (sb.isEmpty()) null else sb.toString().lowercase()
    }

    private fun buildNxDomainPayload(packet: ByteArray, dnsOffset: Int, packetLen: Int): ByteArray {
        val dns = packet.copyOfRange(dnsOffset, packetLen)
        dns[2] = ((dns[2].toInt() and 0x01) or 0x81).toByte()
        dns[3] = 0x83.toByte() // NXDOMAIN
        for (i in 6..11) {
            if (i < dns.size) dns[i] = 0
        }
        return dns
    }

    private fun queryUpstreamResolver(origPacket: ByteArray, dnsOffset: Int, dnsLength: Int): ByteArray? {
        var socket: DatagramSocket? = null
        return try {
            socket = DatagramSocket()
            protect(socket)
            socket.soTimeout = 3000

            val upstreamAddr = InetAddress.getByName(UPSTREAM_DNS)
            socket.send(DatagramPacket(origPacket, dnsOffset, dnsLength, upstreamAddr, 53))

            val respBuf = ByteArray(1500)
            val inPkt = DatagramPacket(respBuf, respBuf.size)
            socket.receive(inPkt)
            respBuf.copyOf(inPkt.length)
        } catch (_: Exception) {
            null
        } finally {
            socket?.close()
        }
    }

    private fun forwardUpstreamIpv4(
        origPacket: ByteArray,
        srcPort: Int,
        dstPort: Int,
        dnsOffset: Int,
        dnsLength: Int,
        output: FileOutputStream
    ) {
        val dnsResp = queryUpstreamResolver(origPacket, dnsOffset, dnsLength) ?: return
        val responsePacket = buildIpv4UdpResponse(origPacket, srcPort, dstPort, dnsResp)
        synchronized(output) {
            try {
                output.write(responsePacket)
            } catch (_: Exception) {
            }
        }
    }

    private fun forwardUpstreamIpv6(
        origPacket: ByteArray,
        srcPort: Int,
        dstPort: Int,
        dnsOffset: Int,
        dnsLength: Int,
        output: FileOutputStream
    ) {
        val dnsResp = queryUpstreamResolver(origPacket, dnsOffset, dnsLength) ?: return
        val responsePacket = buildIpv6UdpResponse(origPacket, srcPort, dstPort, dnsResp)
        synchronized(output) {
            try {
                output.write(responsePacket)
            } catch (_: Exception) {
            }
        }
    }

    private fun buildIpv4UdpResponse(
        origPacket: ByteArray,
        srcPort: Int,
        dstPort: Int,
        dnsPayload: ByteArray
    ): ByteArray {
        val udpLen = 8 + dnsPayload.size
        val totalLen = 20 + udpLen
        val out = ByteArray(totalLen)

        out[0] = 0x45.toByte()
        out[2] = ((totalLen ushr 8) and 0xFF).toByte()
        out[3] = (totalLen and 0xFF).toByte()
        out[4] = origPacket[4]
        out[5] = origPacket[5]
        out[6] = 0x40.toByte()
        out[8] = 64.toByte()
        out[9] = 17.toByte()

        System.arraycopy(origPacket, 16, out, 12, 4)
        System.arraycopy(origPacket, 12, out, 16, 4)

        var sum = 0
        for (i in 0 until 20 step 2) {
            if (i == 10) continue
            sum += ((out[i].toInt() and 0xFF) shl 8) or (out[i + 1].toInt() and 0xFF)
        }
        while ((sum ushr 16) != 0) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        val checksum = sum.inv() and 0xFFFF
        out[10] = ((checksum ushr 8) and 0xFF).toByte()
        out[11] = (checksum and 0xFF).toByte()

        out[20] = ((dstPort ushr 8) and 0xFF).toByte()
        out[21] = (dstPort and 0xFF).toByte()
        out[22] = ((srcPort ushr 8) and 0xFF).toByte()
        out[23] = (srcPort and 0xFF).toByte()
        out[24] = ((udpLen ushr 8) and 0xFF).toByte()
        out[25] = (udpLen and 0xFF).toByte()

        System.arraycopy(dnsPayload, 0, out, 28, dnsPayload.size)
        return out
    }

    private fun buildIpv6UdpResponse(
        origPacket: ByteArray,
        srcPort: Int,
        dstPort: Int,
        dnsPayload: ByteArray
    ): ByteArray {
        val udpLen = 8 + dnsPayload.size
        val totalLen = 40 + udpLen
        val out = ByteArray(totalLen)

        out[0] = 0x60.toByte()
        out[4] = ((udpLen ushr 8) and 0xFF).toByte()
        out[5] = (udpLen and 0xFF).toByte()
        out[6] = 17.toByte() // Next Header = UDP
        out[7] = 64.toByte() // Hop Limit

        // Swap Source IPv6 (8..23) and Destination IPv6 (24..39)
        System.arraycopy(origPacket, 24, out, 8, 16)
        System.arraycopy(origPacket, 8, out, 24, 16)

        // UDP Header
        out[40] = ((dstPort ushr 8) and 0xFF).toByte()
        out[41] = (dstPort and 0xFF).toByte()
        out[42] = ((srcPort ushr 8) and 0xFF).toByte()
        out[43] = (srcPort and 0xFF).toByte()
        out[44] = ((udpLen ushr 8) and 0xFF).toByte()
        out[45] = (udpLen and 0xFF).toByte()

        System.arraycopy(dnsPayload, 0, out, 48, dnsPayload.size)

        // Compute mandatory IPv6 UDP Checksum
        var sum = 0
        for (i in 8 until 40 step 2) {
            sum += ((out[i].toInt() and 0xFF) shl 8) or (out[i + 1].toInt() and 0xFF)
        }
        sum += (udpLen ushr 16) and 0xFFFF
        sum += udpLen and 0xFFFF
        sum += 17 // Next Header UDP

        var i = 40
        while (i < totalLen) {
            val high = out[i].toInt() and 0xFF
            val low = if (i + 1 < totalLen) out[i + 1].toInt() and 0xFF else 0
            sum += (high shl 8) or low
            i += 2
        }
        while ((sum ushr 16) != 0) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        val finalChecksum = (sum.inv() and 0xFFFF).let { if (it == 0) 0xFFFF else it }
        out[46] = ((finalChecksum ushr 8) and 0xFF).toByte()
        out[47] = (finalChecksum and 0xFF).toByte()

        return out
    }

    private fun startForegroundNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "FocusGuard DNS Shield",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("FocusGuard Web Shield Active")
            .setContentText("Blocking adult & distraction domains (IPv4 + IPv6)")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()

        startForeground(NOTIF_ID, notification)
    }

    private fun stopVpn() {
        running = false
        FocusPrefs.setVpnEnabled(this, false)
        try {
            vpnInterface?.close()
        } catch (_: Exception) {
        }
        vpnInterface = null
        pool?.shutdownNow()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
