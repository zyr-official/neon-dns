package com.neon.dns

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors

class DnsVpnService : VpnService() {
    private var tun: ParcelFileDescriptor? = null
    @Volatile private var running = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { stopVpn(); return START_NOT_STICKY }
        val dns = intent?.getStringExtra("dns") ?: return START_NOT_STICKY
        running = false
        tun?.close()
        val fd = Builder()
            .setSession("PingoDNS")
            .addAddress("10.0.0.2", 32)
            .addDnsServer("10.0.0.1")
            .addRoute("10.0.0.1", 32)
            .establish() ?: return START_NOT_STICKY
        tun = fd
        running = true
        VpnState.connected = true
        Thread { forward(fd, dns) }.start()
        return START_STICKY
    }

    private fun forward(fd: ParcelFileDescriptor, dns: String) {
        val inp = FileInputStream(fd.fileDescriptor)
        val out = FileOutputStream(fd.fileDescriptor)
        val target = InetAddress.getByName(dns)
        val pool = Executors.newCachedThreadPool()
        val buf = ByteArray(32767)
        while (running) {
            val n = try { inp.read(buf) } catch (e: Exception) { break }
            if (n < 28) continue
            if ((buf[0].toInt() shr 4) != 4 || buf[9].toInt() != 17) continue
            val ihl = (buf[0].toInt() and 15) * 4
            val pkt = buf.copyOf(n)
            pool.execute { handle(pkt, ihl, target, out) }
        }
        pool.shutdownNow()
    }

    private fun handle(p: ByteArray, ihl: Int, target: InetAddress, out: FileOutputStream) {
        try {
            val dstPort = ((p[ihl + 2].toInt() and 255) shl 8) or (p[ihl + 3].toInt() and 255)
            if (dstPort != 53) return
            val payload = p.copyOfRange(ihl + 8, p.size)
            val s = DatagramSocket()
            protect(s)
            s.soTimeout = 4000
            s.send(DatagramPacket(payload, payload.size, target, 53))
            val rb = ByteArray(4096)
            val rp = DatagramPacket(rb, rb.size)
            s.receive(rp)
            s.close()
            val len = rp.length
            val total = 28 + len
            val r = ByteArray(total)
            r[0] = 0x45
            r[2] = (total shr 8).toByte(); r[3] = total.toByte()
            r[8] = 64; r[9] = 17
            System.arraycopy(p, 16, r, 12, 4)
            System.arraycopy(p, 12, r, 16, 4)
            System.arraycopy(p, ihl + 2, r, 20, 2)
            System.arraycopy(p, ihl, r, 22, 2)
            val ul = 8 + len
            r[24] = (ul shr 8).toByte(); r[25] = ul.toByte()
            System.arraycopy(rb, 0, r, 28, len)
            var sum = 0
            for (i in 0 until 20 step 2) sum += ((r[i].toInt() and 255) shl 8) or (r[i + 1].toInt() and 255)
            while (sum shr 16 != 0) sum = (sum and 0xffff) + (sum shr 16)
            val c = sum.inv() and 0xffff
            r[10] = (c shr 8).toByte(); r[11] = c.toByte()
            synchronized(out) { out.write(r) }
        } catch (e: Exception) { }
    }

    private fun stopVpn() {
        running = false
        VpnState.connected = false
        try { tun?.close() } catch (e: Exception) { }
        tun = null
        stopSelf()
    }

    override fun onRevoke() { stopVpn() }
    override fun onDestroy() { stopVpn(); super.onDestroy() }
}
