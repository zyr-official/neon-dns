package com.neon.dns

import android.content.Context
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

object RemoteConfig {

    // آدرس سرور تنظیمات
    private val bases = listOf("https://dns.pingooshop.ir/")

    // کلید عمومی امضا
    private const val PUB_B64 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE7O5Ulc5eX7vEm/8WKS33xfWeGyAOBmz0MPuptyy/JLwBiFqquEXiUiXYolgnmqOatFnETTs8DgzoxJDyjxXJFQ=="

    private val ipRegex = Regex("^(25[0-5]|2[0-4]\\d|1?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|1?\\d?\\d)){3}$")

    @Volatile
    private var map: Map<String, Pair<String, String>> = emptyMap()

    @Volatile
    private var last = 0L

    // کمترین نسخه مجاز (versionCode). اگه نسخه نصب‌شده کمتر باشه، کاربر باید بروزرسانی کنه
    var minVer by mutableStateOf(0L)
        private set

    fun needsUpdate(): Boolean = minVer > BuildConfig.VERSION_CODE.toLong()

    private class Parsed(val ver: Long, val min: Long, val map: Map<String, Pair<String, String>>)

    fun ipsFor(id: String?): Pair<String, String>? =
        if (id == null) null else map[id]

    fun idByName(name: String): String? =
        servers.firstOrNull {
            it.name.substringBefore(" (") == name
        }?.id

    private fun ok(ip: String): Boolean {
        if (!ipRegex.matches(ip)) return false
        val first = ip.substringBefore('.').toInt()
        return first != 0 && first != 127 && first < 224
    }

    private fun verify(data: ByteArray, sigB64: String): Boolean = try {
        if (PUB_B64.length < 60) {
            false
        } else {
            val pub = KeyFactory.getInstance("EC")
                .generatePublic(X509EncodedKeySpec(Base64.decode(PUB_B64, Base64.DEFAULT)))
            val sg = Signature.getInstance("SHA256withECDSA")
            sg.initVerify(pub)
            sg.update(data)
            sg.verify(Base64.decode(sigB64.trim(), Base64.DEFAULT))
        }
    } catch (e: Exception) {
        false
    }

    private fun parse(text: String): Parsed? = try {
        val root = JSONObject(text)
        val ver = root.getLong("ver")
        val min = root.optLong("min", 0L).coerceIn(0L, 100000L)
        val arr = root.getJSONArray("servers")
        val known = servers.map { it.id }.toSet()
        val out = HashMap<String, Pair<String, String>>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.getString("id")
            val a = o.getString("ip")
            val b = o.optString("ip2", "")
            if (id !in known || !ok(a)) continue
            out[id] = Pair(a, if (ok(b)) b else a)
        }
        Parsed(ver, min, out)
    } catch (e: Exception) {
        null
    }

    private fun get(u: String, max: Int): ByteArray? {
        if (!u.startsWith("https://")) return null
        return try {
            val c = URL(u).openConnection() as HttpURLConnection
            c.connectTimeout = 6000
            c.readTimeout = 6000
            c.useCaches = false
            c.instanceFollowRedirects = false
            if (c.responseCode != 200) {
                c.disconnect()
                return null
            }
            val buf = ByteArrayOutputStream()
            val tmp = ByteArray(4096)
            c.inputStream.use { ins ->
                while (true) {
                    val n = ins.read(tmp)
                    if (n < 0) break
                    buf.write(tmp, 0, n)
                    if (buf.size() > max) throw Exception("big")
                }
            }
            c.disconnect()
            buf.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    // آخرین تنظیمات امضاشده رو موقع باز شدن اپ می‌خونه (حتی آفلاین)
    fun load(ctx: Context) {
        val sp = ctx.applicationContext.getSharedPreferences("remote", 0)
        val t = sp.getString("json", null) ?: return
        val sg = sp.getString("sig", null) ?: return
        if (!verify(t.toByteArray(Charsets.UTF_8), sg)) return
        parse(t)?.let {
            map = it.map
            minVer = it.min
        }
    }

    fun fetchNow(ctx: Context): Boolean {
        val sp = ctx.applicationContext.getSharedPreferences("remote", 0)
        for (b in bases) {
            val jb = get(b + "servers.json", 32768) ?: continue
            val sb = get(b + "servers.sig", 2048) ?: continue
            val sig = String(sb, Charsets.UTF_8)
            if (!verify(jb, sig)) continue
            val text = String(jb, Charsets.UTF_8)
            val p = parse(text) ?: continue
            val old = sp.getLong("ver", 0L)
            if (p.ver < old) continue
            map = p.map
            minVer = p.min
            sp.edit()
                .putString("json", text)
                .putString("sig", sig)
                .putLong("ver", p.ver)
                .apply()
            return p.ver != old
        }
        return false
    }

    fun refresh(ctx: Context, force: Boolean = false, delayMs: Long = 0L) {
        val now = System.currentTimeMillis()
        if (!force && now - last < 60_000) return
        last = now
        val app = ctx.applicationContext
        Thread {
            try {
                if (delayMs > 0) Thread.sleep(delayMs)
            } catch (e: Exception) {
            }
            fetchNow(app)
        }.start()
    }
}
