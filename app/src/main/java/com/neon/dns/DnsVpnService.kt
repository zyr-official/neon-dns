package com.neon.dns

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class DnsVpnService : VpnService() {
    private var tun: ParcelFileDescriptor? = null
    private var pool: ThreadPoolExecutor? = null
    @Volatile private var running = false
    @Volatile private var gen = 0
    @Volatile private var t1: InetAddress? = null
    @Volatile private var t2: InetAddress? = null

    private class Cached(val data: ByteArray, val exp: Long)
    private val cache = ConcurrentHashMap<String, Cached>()

    private val ipRegex = Regex("^(25[0-5]|2[0-4]\\d|1?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|1?\\d?\\d)){3}$")

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { stopVpn(); return START_NOT_STICKY }
        val dns = intent?.getStringExtra("dns")
        if (dns == null || !ipRegex.matches(dns)) return START_NOT_STICKY
        val dns2 = intent.getStringExtra("dns2")?.takeIf { ipRegex.matches(it) }
        val name = intent.getStringExtra("name") ?: ""
        if (RemoteConfig.needsUpdate()) return START_NOT_STICKY // جدید

        // اگه تنظیمات امضاشده از راه دور برای این سرور آی‌پی داشته باشه، همون استفاده می‌شه
        val sid = RemoteConfig.idByName(name)
        val remote = RemoteConfig.ipsFor(sid)
        val a = remote?.first ?: dns
        val b = remote?.second ?: dns2 ?: a

        running = false
        gen++
        val myGen = gen
        pool?.shutdownNow()
        try { tun?.close() } catch (e: Exception) { }
        cache.clear()

        val builder = Builder()
            .setSession("PingoDNS")
            .setBlocking(true)
            .addAddress("10.0.0.2", 32)
            .addDnsServer("10.0.0.1")
            .addRoute("10.0.0.1", 32)
        // خود اپ از VPN مستثنی می‌شه تا بتونه تنظیمات جدید رو حتی وقتی DNS قبلی خرابه بگیره
        try { builder.addDisallowedApplication(packageName) } catch (e: Exception) { }
        val fd = builder.establish() ?: return START_NOT_STICKY

        tun = fd
        t1 = InetAddress.getByName(a)
        t2 = InetAddress.getByName(b)
        running = true
        VpnState.connected = true
        showNote(name)
        val p = ThreadPoolExecutor(2, 12, 30, TimeUnit.SECONDS, ArrayBlockingQueue<Runnable>(128), ThreadPoolExecutor.DiscardPolicy())
        pool = p
        Thread { forward(fd, p) }.start()

        // هر ۳۰ ثانیه تنظیمات رو چک می‌کنه و اگه آی‌پی عوض شده بود فوری جایگزین می‌کنه
        Thread {
            while (running && gen == myGen) {
                try { Thread.sleep(30_000) } catch (e: InterruptedException) { break }
                if (!running || gen != myGen) break
                try {
                    RemoteConfig.fetchNow(this)
                    if (RemoteConfig.needsUpdate()) { stopVpn(); break } // جدید
                    val r = RemoteConfig.ipsFor(sid)
                    if (r != null) {
                        val n1 = InetAddress.getByName(r.first)
                        val n2 = InetAddress.getByName(r.second)
                        if (n1 != t1 || n2 != t2) {
                            t1 = n1
                            t2 = n2
                            cache.clear()
                        }
                    }
                } catch (e: Exception) { }
            }
        }.also { it.isDaemon = true }.start()
        return START_NOT_STICKY
    }

    private fun showNote(name: String) {
        try {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            if (Build.VERSION.SDK_INT >= 26) {
                nm.createNotificationChannel(
                    NotificationChannel("pingo_status", "وضعیت اتصال", NotificationManager.IMPORTANCE_LOW)
                )
            }
            val open = PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE
            )
            val stop = PendingIntent.getService(
                this, 1, Intent(this, DnsVpnService::class.java).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE
            )
            val n = NotificationCompat.Builder(this, "pingo_status")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("PingoDNS متصل است")
                .setContentText(name)
                .setContentIntent(open)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(0, "قطع اتصال", stop)
                .build()
            nm.notify(1, n)
        } catch (e: Exception) { }
    }

    private fun forward(fd: ParcelFileDescriptor, p: ThreadPoolExecutor) {
        val inp = FileInputStream(fd.fileDescriptor)
        val out = FileOutputStream(fd.fileDescriptor)
        val buf = ByteArray(32767)
        while (running) {
            val n = try { inp.read(buf) } catch (e: Exception) { break }
            if (n < 0) break
            if (n < 28) continue
            if ((buf[0].toInt() shr 4) != 4) continue
            val ihl = (buf[0].toInt() and 15) * 4
            if (ihl < 20 || n < ihl + 8) continue
            if (buf[9].toInt() != 17) continue
            if (((buf[6].toInt() and 0x3F) or (buf[7].toInt() and 0xFF)) != 0) continue
            val tl = ((buf[2].toInt() and 255) shl 8) or (buf[3].toInt() and 255)
            if (tl > n || tl < ihl + 8 + 12 || tl - ihl - 8 > 1500) continue
            if (buf[16].toInt() != 10 || buf[17].toInt() != 0 || buf[18].toInt() != 0 || buf[19].toInt() != 1) continue
            val dport = ((buf[ihl + 2].toInt() and 255) shl 8) or (buf[ihl + 3].toInt() and 255)
            if (dport != 53) continue
            val x1 = t1 ?: continue
            val x2 = t2
            val pkt = buf.copyOf(tl)
            p.execute { handle(pkt, ihl, tl, x1, x2, out) }
        }
        p.shutdownNow()
    }

    private fun ask(payload: ByteArray, target: InetAddress): ByteArray? {
        val s = DatagramSocket()
        return try {
            protect(s)
            s.soTimeout = 2500
            s.connect(target, 53)
            s.send(DatagramPacket(payload, payload.size))
            val rb = ByteArray(4096)
            val rp = DatagramPacket(rb, rb.size)
            s.receive(rp)
            if (rp.length < 12 || rb[0] != payload[0] || rb[1] != payload[1]) null else rb.copyOf(rp.length)
        } catch (e: Exception) { null } finally { s.close() }
    }

    private fun handle(p: ByteArray, ihl: Int, tl: Int, a: InetAddress, b: InetAddress?, out: FileOutputStream) {
        try {
            val payload = p.copyOfRange(ihl + 8, tl)
            val key = String(payload, 2, payload.size - 2, Charsets.ISO_8859_1)
            val now = System.currentTimeMillis()
            var resp: ByteArray? = null
            val c = cache[key]
            if (c != null && c.exp > now) {
                resp = c.data.copyOf()
                resp[0] = payload[0]; resp[1] = payload[1]
            } else {
                resp = ask(payload, a)
                if (resp == null && b != null) resp = ask(payload, b)
                if (resp != null && (resp[3].toInt() and 0x0F) == 0 && (resp[6].toInt() != 0 || resp[7].toInt() != 0)) {
                    if (cache.size > 400) cache.clear()
                    cache[key] = Cached(resp.copyOf(), now + 30_000)
                }
            }
            if (resp == null || resp.size > 1400) return
            val len = resp.size
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
            System.arraycopy(resp, 0, r, 28, len)
            var sum = 0
            for (i in 0 until 20 step 2) sum += ((r[i].toInt() and 255) shl 8) or (r[i + 1].toInt() and 255)
            while (sum shr 16 != 0) sum = (sum and 0xffff) + (sum shr 16)
            val ck = sum.inv() and 0xffff
            r[10] = (ck shr 8).toByte(); r[11] = ck.toByte()
            synchronized(out) { out.write(r) }
        } catch (e: Exception) { }
    }

    private fun stopVpn() {
        running = false
        gen++
        VpnState.connected = false
        pool?.shutdownNow()
        pool = null
        try { tun?.close() } catch (e: Exception) { }
        tun = null
        cache.clear()
        try { getSystemService(NotificationManager::class.java)?.cancel(1) } catch (e: Exception) { }
        stopSelf()
    }

    override fun onRevoke() { stopVpn() }
    override fun onDestroy() { stopVpn(); super.onDestroy() }
}
