package com.neon.dns

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

const val TELEGRAM_ID = "NpvTunnel111"

val Cyan = Color(0xFF00E5FF)
val Purple = Color(0xFFB026FF)
val CardBg = Color(0xFF0F2040)
val Muted = Color(0xFF8FA3C8)

data class Srv(val id: String, val name: String, val desc: String, val ip: String, val cat: String, val tags: String = "")

val cats = listOf("همه", "علاقه‌مندی‌ها", "اختصاصی", "بتل‌رویال", "شوتر", "ورزشی", "استراتژی", "عمومی")

val servers = listOf(
    Srv("rdns1", "سرور اختصاصی ۱", "پینگ فوق‌العاده و پایدار", "78.157.42.100", "اختصاصی"),
    Srv("rdns2", "سرور اختصاصی ۲", "سرعت بالا، مسیریابی گیمینگ", "10.202.10.10", "اختصاصی"),
    Srv("delta", "سرور دلتافورس (Delta Force)", "پینگ سبز بدون رجیستری لگ", "78.157.42.100", "شوتر", "delta force دلتا"),
    Srv("so2", "سرور Standoff 2", "مخصوص رقابت‌های تورنمنت", "10.202.10.10", "شوتر", "standoff استندآف"),
    Srv("cod", "سرور کالاف دیوتی (Call of Duty)", "ثبات پینگ برای بتل‌رویال و مولتی", "1.1.1.1", "شوتر", "cod کالاف"),
    Srv("wz", "سرور وارزون موبایل (Warzone)", "ترافیک اختصاصی وارزون", "8.8.8.8", "شوتر", "warzone وارزون"),
    Srv("pubg1", "سرور پابجی موبایل ۱ (PUBG)", "سرور اصلی اروپا و خاورمیانه", "1.1.1.1", "بتل‌رویال", "pubg پابجی"),
    Srv("pubg2", "سرور پابجی موبایل ۲ (PUBG)", "پینگ پایدار برای رنک", "8.8.8.8", "بتل‌رویال", "pubg پابجی"),
    Srv("ff", "سرور فری فایر (Free Fire)", "اتصال پایدار بدون پاکت لاست", "9.9.9.9", "بتل‌رویال", "free fire فری فایر"),
    Srv("ef1", "سرور EFootball ۱", "بدون تاخیر و شوت آنلاین", "1.1.1.1", "ورزشی", "efootball ای فوتبال pes"),
    Srv("ef2", "سرور EFootball ۲", "مخصوص مسابقات آنلاین", "8.8.8.8", "ورزشی", "efootball ای فوتبال pes"),
    Srv("fc", "سرور FC Mobile", "اتصال پایدار مسابقات Ultimate", "9.9.9.9", "ورزشی", "fc mobile فیفا fifa"),
    Srv("coc1", "سرور کلش آف کلنز ۱", "بدون دیسکانکتی در اتک", "1.1.1.1", "استراتژی", "clash of clans coc کلش"),
    Srv("coc2", "سرور کلش آف کلنز ۲", "سرور پشتیبان وار کلن", "8.8.8.8", "استراتژی", "clash of clans coc کلش"),
    Srv("cf", "Cloudflare", "سریع و امن", "1.1.1.1", "عمومی"),
    Srv("gg", "Google", "پایدار و جهانی", "8.8.8.8", "عمومی"),
    Srv("q9", "Quad9", "امنیت بالا", "9.9.9.9", "عمومی"),
    Srv("od", "OpenDNS", "پایداری بالا", "208.67.222.222", "عمومی"),
    Srv("shecan", "Shecan", "دسترسی به سرویس‌ها", "178.22.122.100", "عمومی", "شکن"),
    Srv("electro", "Electro", "مخصوص گیمینگ", "78.157.42.100", "عمومی", "الکترو"),
    Srv("radar", "Radar Game", "پینگ پایین بازی", "10.202.10.10", "عمومی", "رادار"),
    Srv("403", "403.online", "دور زدن تحریم", "10.202.10.202", "عمومی"),
)

object VpnState { var connected by mutableStateOf(false) }

fun ping(ip: String): Int = try {
    val q = byteArrayOf(0x12, 0x34, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0) +
        "\u0006google\u0003com\u0000".toByteArray(Charsets.ISO_8859_1) + byteArrayOf(0, 1, 0, 1)
    val s = DatagramSocket()
    s.soTimeout = 2500
    val t0 = System.nanoTime()
    s.send(DatagramPacket(q, q.size, InetAddress.getByName(ip), 53))
    s.receive(DatagramPacket(ByteArray(512), 512))
    s.close()
    ((System.nanoTime() - t0) / 1_000_000).toInt()
} catch (e: Exception) { -1 }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { App() } }
    }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("neon", 0) }
    var sel by remember { mutableStateOf(servers.firstOrNull { it.id == prefs.getString("sel", "") } ?: servers[0]) }
    val favs = remember { mutableStateListOf<String>().apply { addAll(prefs.getStringSet("favs", emptySet())!!) } }
    val pings = remember { mutableStateMapOf<String, Int>() }
    var query by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("همه") }
    var showAbout by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current

    fun start(ip: String) { ctx.startService(Intent(ctx, DnsVpnService::class.java).putExtra("dns", ip)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) start(sel.ip)
    }
    fun toggle() {
        if (VpnState.connected) {
            ctx.startService(Intent(ctx, DnsVpnService::class.java).setAction("STOP"))
        } else {
            val i = VpnService.prepare(ctx)
            if (i != null) launcher.launch(i) else start(sel.ip)
        }
    }
    fun measure() {
        pings.clear()
        servers.map { it.ip }.distinct().forEach { ip -> scope.launch(Dispatchers.IO) { pings[ip] = ping(ip) } }
    }
    LaunchedEffect(Unit) { measure() }

    val on = VpnState.connected
    val col = if (on) Cyan else Color(0xFF5A6A8A)
    val t = rememberInfiniteTransition(label = "p")
    val pulse by t.animateFloat(
        0.94f, 1.06f,
        infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "a"
    )

    val q = query.trim().lowercase()
    val shown = servers.filter { s ->
        (cat == "همه" || (cat == "علاقه‌مندی‌ها" && s.id in favs) || s.cat == cat) &&
            (q.isEmpty() || (s.name + " " + s.desc + " " + s.tags).lowercase().contains(q))
    }.sortedByDescending { it.id in favs }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF050B1A), Color(0xFF0B1B3A), Color(0xFF14123A))))) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("PINGO DNS", color = Cyan, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp, modifier = Modifier.weight(1f))
                    Text("v1.1", color = Cyan, fontSize = 13.sp,
                        modifier = Modifier.border(1.dp, Cyan, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 4.dp))
                }
                Spacer(Modifier.height(16.dp))
                val cardShape = RoundedCornerShape(24.dp)
                Row(
                    Modifier.fillMaxWidth().clip(cardShape).background(CardBg.copy(alpha = .8f))
                        .border(1.5.dp, Brush.horizontalGradient(listOf(Cyan, Purple)), cardShape).padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("سرور انتخاب شده", color = Muted, fontSize = 13.sp)
                        Text(sel.name, color = Cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("وضعیت", color = Muted, fontSize = 13.sp)
                        Text(if (on) "متصل شد" else "متصل نیست",
                            color = if (on) Color(0xFF00FF9C) else Color(0xFFFF3D6E), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(220.dp).scale(if (on) pulse else 1f)
                            .border(2.dp, Brush.sweepGradient(listOf(Cyan, Purple, Cyan)), CircleShape))
                        Box(
                            Modifier.size(160.dp)
                                .shadow(if (on) 36.dp else 8.dp, CircleShape, ambientColor = col, spotColor = col)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(Color(0xFF1B3358), Color(0xFF0A1428))))
                                .border(3.dp, col, CircleShape)
                                .clickable { toggle() },
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(Modifier.size(64.dp)) {
                                val sw = 9.dp.toPx()
                                val off = 6.dp.toPx()
                                drawArc(color = col, startAngle = -50f, sweepAngle = 280f, useCenter = false,
                                    topLeft = Offset(sw / 2, sw / 2 + off), size = Size(size.width - sw, size.width - sw - off),
                                    style = Stroke(sw, cap = StrokeCap.Round))
                                drawLine(col, Offset(size.width / 2, sw / 2), Offset(size.width / 2, size.height * 0.45f), sw, StrokeCap.Round)
                            }
                        }
                    }
                }
                Text(if (on) "برای قطع اتصال لمس کنید" else "✦ با یک لمس روی دکمه بالا، متصل شوید ✦",
                    color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("انتخاب سرور اختصاصی بازی", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("↻ پینگ", color = Cyan, fontSize = 14.sp,
                        modifier = Modifier.clip(RoundedCornerShape(50)).clickable { measure() }.padding(8.dp))
                }
                Spacer(Modifier.height(10.dp))
                val sh = RoundedCornerShape(18.dp)
                BasicTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                    cursorBrush = SolidColor(Cyan),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Row(Modifier.fillMaxWidth().clip(sh).background(CardBg).border(1.dp, Color(0xFF1E3358), sh).padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("🔍", fontSize = 16.sp)
                            Spacer(Modifier.width(10.dp))
                            Box(Modifier.weight(1f)) {
                                if (query.isEmpty()) Text("جستجوی نام بازی (مثلا: پابجی، کالاف...)", color = Muted, fontSize = 14.sp)
                                inner()
                            }
                        }
                    }
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(cats) { c ->
                        val a = c == cat
                        Text(c, color = if (a) Color(0xFF050B1A) else Cyan, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(50))
                                .background(if (a) Cyan else Color.Transparent)
                                .border(1.dp, Cyan, RoundedCornerShape(50))
                                .clickable { cat = c }.padding(horizontal = 14.dp, vertical = 6.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            if (shown.isEmpty()) item { Text("موردی پیدا نشد", color = Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)) }
            items(shown, key = { it.id }) { s ->
                ServerRow(s, s.id == sel.id, pings[s.ip], s.id in favs,
                    onFav = {
                        if (s.id in favs) favs.remove(s.id) else favs.add(s.id)
                        prefs.edit().putStringSet("favs", favs.toSet()).apply()
                    },
                    onClick = {
                        sel = s
                        prefs.edit().putString("sel", s.id).apply()
                        if (VpnState.connected) start(s.ip)
                    })
            }
            item {
                Spacer(Modifier.height(16.dp))
                val tg = RoundedCornerShape(24.dp)
                Row(
                    Modifier.fillMaxWidth().clip(tg).background(CardBg.copy(alpha = .8f))
                        .border(1.5.dp, Brush.horizontalGradient(listOf(Purple, Cyan)), tg)
                        .clickable { uri.openUri("https://t.me/$TELEGRAM_ID") }.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("ارتباط با ما در تلگرام", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("@$TELEGRAM_ID", color = Cyan, fontSize = 15.sp)
                    }
                    Text("✈", color = Cyan, fontSize = 26.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text("درباره ما", color = Purple, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .border(1.dp, Purple, RoundedCornerShape(20.dp)).clickable { showAbout = true }.padding(16.dp))
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    if (showAbout) AlertDialog(
        onDismissRequest = { showAbout = false },
        containerColor = CardBg, titleContentColor = Cyan, textContentColor = Color.White,
        title = { Text("PingoDNS") },
        text = { Text("برنامه تغییر DNS مخصوص گیمینگ.\nفقط ترافیک DNS از سرور انتخابی عبور می‌کند و بقیه اینترنت شما تغییری نمی‌کند.") },
        confirmButton = { Text("باشه", color = Cyan, modifier = Modifier.clickable { showAbout = false }.padding(12.dp)) }
    )
}

@Composable
fun ServerRow(s: Srv, selected: Boolean, ping: Int?, fav: Boolean, onFav: () -> Unit, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val line = if (selected) Brush.horizontalGradient(listOf(Cyan, Purple))
    else Brush.horizontalGradient(listOf(Color(0xFF1E3358), Color(0xFF1E3358)))
    val chip: Pair<String, Color> = when {
        ping == null -> "..." to Color.Gray
        ping < 0 -> "—" to Color(0xFFFF5470)
        ping < 80 -> "${ping}ms" to Color(0xFF00FF9C)
        ping < 150 -> "${ping}ms" to Color(0xFFFFD43B)
        else -> "${ping}ms" to Color(0xFFFF8A3D)
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(shape).background(CardBg.copy(alpha = .85f))
            .border(if (selected) 2.dp else 1.dp, line, shape).clickable(onClick = onClick).padding(start = 8.dp, top = 12.dp, bottom = 12.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (fav) "★" else "☆", color = if (fav) Color(0xFFFFD43B) else Muted, fontSize = 22.sp,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onFav).padding(8.dp))
        Column(Modifier.weight(1f)) {
            Text(s.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(s.desc, color = Muted, fontSize = 13.sp)
            Text(s.ip, color = Cyan.copy(alpha = .7f), fontSize = 12.sp)
        }
        Text(chip.first, color = chip.second, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.border(1.dp, chip.second, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 5.dp))
    }
}
