package com.neon.dns

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.math.cos
import kotlin.math.sin

const val TELEGRAM_ID = "NpvTunnel111"

val Cyan = Color(0xFF00E5FF)
val Blue = Color(0xFF2F7BFF)
val Purple = Color(0xFFB026FF)
val Gold = Color(0xFFE9C987)
val Muted = Color(0xFF9AA8D0)
val Green = Color(0xFF00FF7F)
val Red = Color(0xFFFF3D6E)
val palette = listOf(Color(0xFF00E5FF), Color(0xFF2F7BFF), Color(0xFFB026FF), Color(0xFFFF2DAA))

fun cycle(p: Float, off: Float): Color {
    val x = (((p + off) % 1f + 1f) % 1f) * palette.size
    val i = x.toInt() % palette.size
    val f = x - x.toInt()
    return lerp(palette[i], palette[(i + 1) % palette.size], f)
}

fun Modifier.cycBorder(hue: State<Float>, shape: Shape, w: Dp): Modifier = this.drawBehind {
    val o = shape.createOutline(size, layoutDirection, this)
    val a = cycle(hue.value, 0f)
    val b = cycle(hue.value, .5f)
    drawOutline(o, Brush.horizontalGradient(listOf(a, b, a)), style = Stroke(w.toPx() * 2))
}

data class Srv(
    val id: String, val name: String, val desc: String, val ip: String,
    val cat: String, val tags: String = "", val ab: String = ""
)

val cats = listOf("همه", "علاقه‌مندی‌ها", "اختصاصی", "بتل‌رویال", "شوتر", "ورزشی", "استراتژی", "عمومی")

val servers = listOf(
    Srv("rdns1", "سرور اختصاصی ۱", "پینگ فوق‌العاده و پایدار", "78.157.42.100", "اختصاصی", "", "P1"),
    Srv("rdns2", "سرور اختصاصی ۲", "سرعت بالا، مسیریابی گیمینگ", "10.202.10.10", "اختصاصی", "", "P2"),
    Srv("wz", "سرور وارزون موبایل (Warzone)", "پینگ بهتر برای رجیستری لگ", "8.8.8.8", "شوتر", "warzone وارزون", "WZ"),
    Srv("so2", "سرور استندوف ۲ (Standoff 2)", "مخصوص رقابت‌های تورنمنت", "10.202.10.10", "شوتر", "standoff استندآف", "S2"),
    Srv("cod", "سرور کالاف دیوتی (Call of Duty)", "ثبات پینگ برای بتل رویال و مولتی", "1.1.1.1", "شوتر", "cod کالاف", "COD"),
    Srv("delta", "سرور دلتافورس (Delta Force)", "پینگ سبز بدون رجیستری لگ", "78.157.42.100", "شوتر", "delta force دلتا", "DF"),
    Srv("pubg1", "سرور پابجی موبایل ۱ (PUBG)", "سرور اصلی اروپا و خاورمیانه", "1.1.1.1", "بتل‌رویال", "pubg پابجی", "PG1"),
    Srv("pubg2", "سرور پابجی موبایل ۲ (PUBG)", "سرور پشتیبان و ارزان", "8.8.8.8", "بتل‌رویال", "pubg پابجی", "PG2"),
    Srv("ff", "سرور فری فایر (Free Fire)", "اتصال پایدار بدون پاکت لاست", "9.9.9.9", "بتل‌رویال", "free fire فری فایر", "FF"),
    Srv("ef1", "سرور EFootball ۱", "بدون تاخیر و شوت آنلاین", "1.1.1.1", "ورزشی", "efootball ای فوتبال pes", "EF1"),
    Srv("ef2", "سرور EFootball ۲", "مخصوص مسابقات آنلاین", "8.8.8.8", "ورزشی", "efootball ای فوتبال pes", "EF2"),
    Srv("fc", "سرور FC Mobile", "اتصال پایدار مسابقات Ultimate", "9.9.9.9", "ورزشی", "fc mobile فیفا fifa", "FC"),
    Srv("coc1", "سرور کلش آف کلنز ۱", "بدون دیسکانکتی در اتک", "1.1.1.1", "استراتژی", "clash of clans coc کلش", "CC1"),
    Srv("coc2", "سرور کلش آف کلنز ۲", "سرور پشتیبان وار کلن", "8.8.8.8", "استراتژی", "clash of clans coc کلش", "CC2"),
    Srv("cf", "Cloudflare", "سریع و امن", "1.1.1.1", "عمومی", "", "CF"),
    Srv("gg", "Google", "پایدار و جهانی", "8.8.8.8", "عمومی", "", "G"),
    Srv("q9", "Quad9", "امنیت بالا", "9.9.9.9", "عمومی", "", "Q9"),
    Srv("od", "OpenDNS", "پایداری بالا", "208.67.222.222", "عمومی", "", "OD"),
    Srv("shecan", "Shecan", "دسترسی به سرویس‌ها", "178.22.122.100", "عمومی", "شکن", "SH"),
    Srv("electro", "Electro", "مخصوص گیمینگ", "78.157.42.100", "عمومی", "الکترو", "EL"),
    Srv("radar", "Radar Game", "پینگ پایین بازی", "10.202.10.10", "عمومی", "رادار", "RD"),
    Srv("403", "403.online", "دور زدن تحریم", "10.202.10.202", "عمومی", "", "403"),
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

fun catColors(c: String): List<Color> = when (c) {
    "اختصاصی" -> listOf(Cyan, Purple)
    "شوتر" -> listOf(Color(0xFFFF5470), Color(0xFFFF8A3D))
    "بتل‌رویال" -> listOf(Color(0xFFFFB400), Color(0xFFFF6A00))
    "ورزشی" -> listOf(Color(0xFF00E58F), Color(0xFF0097FF))
    "استراتژی" -> listOf(Color(0xFFB026FF), Color(0xFF5B5BFF))
    else -> listOf(Color(0xFF3B5BDB), Color(0xFF00B7FF))
}

fun starPath(cx: Float, cy: Float, r: Float): Path {
    val p = Path()
    val ir = r * 0.45f
    for (i in 0 until 10) {
        val rad = if (i % 2 == 0) r else ir
        val a = Math.PI / 5 * i - Math.PI / 2
        val x = cx + (rad * Math.cos(a)).toFloat()
        val y = cy + (rad * Math.sin(a)).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    return p
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { App() } }
    }
}

@Composable
fun drawableId(name: String): Int {
    val ctx = LocalContext.current
    return remember(name) { ctx.resources.getIdentifier(name, "drawable", ctx.packageName) }
}

@Composable
fun Badge(s: Srv, size: Dp) {
    val rid = drawableId(if (s.cat == "اختصاصی") "pingo_icon" else "g_" + s.id)
    val shape = RoundedCornerShape(size * 0.24f)
    if (rid != 0) {
        Image(
            painter = painterResource(rid), contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(shape).background(Color(0xFF0D0D0D))
        )
    } else {
        Box(
            Modifier.size(size).clip(shape).background(Brush.linearGradient(catColors(s.cat))),
            contentAlignment = Alignment.Center
        ) {
            val f = if (s.ab.length > 2) 0.26f else 0.34f
            Text(s.ab, color = Color.White, fontWeight = FontWeight.Black, fontSize = (size.value * f).sp, maxLines = 1)
        }
    }
}

@Composable
fun Bars(level: Int, c: Color) {
    Canvas(Modifier.size(22.dp, 16.dp)) {
        val bw = size.width / 7f
        for (i in 0 until 4) {
            val h = size.height * (0.35f + 0.2f * i)
            drawRoundRect(
                color = if (i < level) c else Color.White.copy(alpha = .18f),
                topLeft = Offset(i * bw * 2f, size.height - h),
                size = Size(bw, h),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }
    }
}

@Composable
fun GearIcon(col: Color) {
    Canvas(Modifier.size(32.dp)) {
        val c = center
        val r = size.width * .24f
        drawCircle(col, r, c, style = Stroke(size.width * .12f))
        for (i in 0 until 8) {
            val ang = Math.toRadians(i * 45.0)
            val dx = cos(ang).toFloat()
            val dy = sin(ang).toFloat()
            drawLine(col, Offset(c.x + dx * r * 1.4f, c.y + dy * r * 1.4f),
                Offset(c.x + dx * r * 1.9f, c.y + dy * r * 1.9f), size.width * .15f, StrokeCap.Round)
        }
    }
}

@Composable
fun PowerIcon(col: Color, sz: Dp) {
    Canvas(Modifier.size(sz)) {
        val w = size.width
        val h = size.height
        val sw = w * .12f
        val d = w * .78f
        drawArc(color = col, startAngle = -50f, sweepAngle = 280f, useCenter = false,
            topLeft = Offset((w - d) / 2, h - d - sw / 2), size = Size(d, d),
            style = Stroke(sw, cap = StrokeCap.Round))
        drawLine(col, Offset(w / 2, sw / 2), Offset(w / 2, h * .5f), sw, StrokeCap.Round)
    }
}

@Composable
fun Chevron(col: Color) {
    Canvas(Modifier.size(16.dp, 22.dp)) {
        val p = Path().apply {
            moveTo(size.width * .8f, size.height * .1f)
            lineTo(size.width * .2f, size.height * .5f)
            lineTo(size.width * .8f, size.height * .9f)
        }
        drawPath(p, col, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
fun MainScreen(
    on: Boolean, sel: Srv, hue: State<Float>, rot: State<Float>, pulse: State<Float>,
    onToggle: () -> Unit, onOpenSheet: () -> Unit, onSettings: () -> Unit
) {
    val a = cycle(hue.value, 0f)
    val b = cycle(hue.value, .5f)
    val logoId = drawableId("pingo_logo")

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.Top) {
                Spacer(Modifier.width(48.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (logoId != 0) {
                        Image(painter = painterResource(logoId), contentDescription = null,
                            modifier = Modifier.height(112.dp))
                    } else {
                        Text("Pingo", color = Gold, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("DNS", color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 10.sp,
                        modifier = Modifier.padding(top = 2.dp, start = 10.dp))
                }
                Box(Modifier.size(48.dp).clip(CircleShape).clickable { onSettings() }, contentAlignment = Alignment.Center) {
                    GearIcon(Blue)
                }
            }
        }

        val ps = RoundedCornerShape(44.dp)
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp).height(78.dp).clip(ps)
                .background(Brush.horizontalGradient(listOf(Color(0xFF16103F), Color(0xFF0A0A24))))
                .cycBorder(hue, ps, 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).background(if (on) Green else Red, CircleShape))
                    Spacer(Modifier.width(7.dp))
                    Text("وضعیت اتصال", color = Color.White, fontSize = 14.sp)
                }
                Text(if (on) "DNS متصل شد" else "متصل نیست", color = if (on) Green else Red,
                    fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(22.dp))
        Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(300.dp)
                    .graphicsLayer { val s = if (on) pulse.value else 1f; scaleX = s; scaleY = s }
                    .background(Brush.radialGradient(listOf(a.copy(alpha = if (on) .38f else .2f), Color.Transparent)), CircleShape)
            )
            Box(
                Modifier.size(272.dp).graphicsLayer { rotationZ = -rot.value }
                    .border(1.5.dp, Brush.sweepGradient(listOf(a.copy(alpha = .8f), Color.Transparent, b.copy(alpha = .8f), Color.Transparent, a.copy(alpha = .8f))), CircleShape)
            )
            Box(
                Modifier.size(226.dp).graphicsLayer { rotationZ = rot.value }
                    .border(9.dp, Brush.sweepGradient(listOf(a, b, a)), CircleShape)
            )
            Box(
                Modifier.size(200.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFF16163F), Color(0xFF06061A))))
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) { PowerIcon(Color.White, 84.dp) }
        }
        Spacer(Modifier.height(6.dp))
        Text(if (on) "قطع اتصال DNS" else "اتصال به DNS", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)

        Spacer(Modifier.height(26.dp))
        val cs = RoundedCornerShape(26.dp)
        Row(
            Modifier.fillMaxWidth().clip(cs).background(Color.White.copy(alpha = .05f))
                .cycBorder(hue, cs, 1.5.dp).clickable { onOpenSheet() }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Badge(sel, 52.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("سرور انتخاب شده", color = Muted, fontSize = 12.sp)
                Text(sel.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2)
            }
            Text("انتخاب سرور", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                modifier = Modifier.clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(a, b)))
                    .padding(horizontal = 16.dp, vertical = 9.dp))
        }
        Spacer(Modifier.height(24.dp))
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
    var showSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current
    val on = VpnState.connected

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
    BackHandler(enabled = showSheet) { showSheet = false }

    val t = rememberInfiniteTransition(label = "t")
    val hue = t.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "h")
    val rot = t.animateFloat(0f, 360f, infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "r")
    val pulse = t.animateFloat(0.95f, 1.05f,
        infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")

    val q = query.trim().lowercase()
    val shown = servers.filter { s ->
        (cat == "همه" || (cat == "علاقه‌مندی‌ها" && s.id in favs) || s.cat == cat) &&
            (q.isEmpty() || (s.name + " " + s.desc + " " + s.tags).lowercase().contains(q))
    }.sortedByDescending { it.id in favs }

    Box(
        Modifier.fillMaxSize().background(Color(0xFF05040F)).drawBehind {
            val c1 = cycle(hue.value, 0.2f)
            val c2 = cycle(hue.value, 0.7f)
            drawCircle(Brush.radialGradient(listOf(c1.copy(alpha = .38f), Color.Transparent),
                center = Offset(0f, size.height * .12f), radius = size.width * .8f),
                radius = size.width * .8f, center = Offset(0f, size.height * .12f))
            drawCircle(Brush.radialGradient(listOf(c2.copy(alpha = .3f), Color.Transparent),
                center = Offset(size.width, size.height * .5f), radius = size.width * .8f),
                radius = size.width * .8f, center = Offset(size.width, size.height * .5f))
        }
    ) {
        MainScreen(on, sel, hue, rot, pulse,
            onToggle = { toggle() }, onOpenSheet = { showSheet = true }, onSettings = { showAbout = true })

        AnimatedVisibility(showSheet, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .6f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showSheet = false })
        }
        AnimatedVisibility(
            showSheet, modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it }, exit = slideOutVertically { it }
        ) {
            val sheetShape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
            Column(
                Modifier.fillMaxWidth().fillMaxHeight(0.8f).clip(sheetShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFF0B1436), Color(0xFF0E0A2C))))
                    .cycBorder(hue, sheetShape, 2.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                    .navigationBarsPadding().padding(horizontal = 16.dp)
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp).size(54.dp, 5.dp)
                    .background(Color.White.copy(alpha = .35f), RoundedCornerShape(50)))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Canvas(Modifier.size(34.dp)) {
                                val c = center
                                val r = size.width * .42f
                                drawCircle(Blue, r, c, style = Stroke(2.5.dp.toPx()))
                                drawOval(color = Blue, topLeft = Offset(c.x - r * .45f, c.y - r), size = Size(r * .9f, r * 2), style = Stroke(2.dp.toPx()))
                                drawLine(Blue, Offset(c.x - r, c.y), Offset(c.x + r, c.y), 2.dp.toPx())
                            }
                            Spacer(Modifier.width(10.dp))
                            Text("انتخاب سرور DNS", color = Cyan, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            Modifier.size(52.dp).clip(CircleShape).border(1.5.dp, Purple, CircleShape)
                                .clickable { showSheet = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(Modifier.size(20.dp)) {
                                drawLine(Color.White, Offset(0f, 0f), Offset(size.width, size.height), 3.dp.toPx(), StrokeCap.Round)
                                drawLine(Color.White, Offset(size.width, 0f), Offset(0f, size.height), 3.dp.toPx(), StrokeCap.Round)
                            }
                        }
                    }
                }
                Text("برای کاهش پینگ و ثبت رکوردهای روان سرور مورد نظر را انتخاب کنید",
                    color = Color.White.copy(alpha = .9f), fontSize = 13.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
                BasicTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                    cursorBrush = SolidColor(Cyan),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Color(0xFF1A2145)).padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Canvas(Modifier.size(22.dp)) {
                                drawCircle(Blue, radius = size.width * 0.34f, center = Offset(size.width * 0.42f, size.height * 0.42f), style = Stroke(2.5.dp.toPx()))
                                drawLine(Blue, Offset(size.width * 0.68f, size.height * 0.68f), Offset(size.width * 0.95f, size.height * 0.95f), 2.5.dp.toPx(), StrokeCap.Round)
                            }
                            Spacer(Modifier.width(12.dp))
                            Box(Modifier.weight(1f)) {
                                if (query.isEmpty()) Text("جستجوی نام بازی، (مثلا: پابجی، کالاف...)", color = Muted, fontSize = 14.sp)
                                inner()
                            }
                        }
                    }
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(cats) { c ->
                        val act = c == cat
                        val cs = RoundedCornerShape(50)
                        Text(c, color = if (act) Color(0xFF050B1A) else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(cs)
                                .background(if (act) Brush.horizontalGradient(listOf(Cyan, Color(0xFF7AF0FF))) else Brush.horizontalGradient(listOf(Color.White.copy(alpha = .08f), Color.White.copy(alpha = .08f))))
                                .clickable { cat = c }.padding(horizontal = 15.dp, vertical = 7.dp))
                    }
                }
                LazyColumn(Modifier.weight(1f).padding(top = 6.dp)) {
                    if (shown.isEmpty()) item {
                        Text("موردی پیدا نشد", color = Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp))
                    }
                    items(shown, key = { it.id }) { s ->
                        val selected = s.id == sel.id
                        val fav = s.id in favs
                        val p = pings[s.ip]
                        val level = when { p == null || p < 0 -> 0; p < 60 -> 4; p < 100 -> 3; p < 160 -> 2; else -> 1 }
                        val pc = when { p == null -> Color.Gray; p < 0 -> Red; p < 100 -> Green; p < 160 -> Color(0xFFFFD43B); else -> Color(0xFFFF8A3D) }
                        val shape = RoundedCornerShape(24.dp)
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(shape)
                                .background(if (selected) Cyan.copy(alpha = .12f) else Color(0xFF0A0F2C))
                                .cycBorder(hue, shape, 1.5.dp)
                                .clickable {
                                    sel = s
                                    prefs.edit().putString("sel", s.id).apply()
                                    if (VpnState.connected) start(s.ip)
                                    showSheet = false
                                }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Badge(s, 54.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2)
                                Text(s.desc, color = Muted, fontSize = 13.sp, maxLines = 1)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Bars(level, pc)
                                Text(if (p == null) "..." else if (p < 0) "—" else "${p}ms", color = pc, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                Modifier.clip(CircleShape).clickable {
                                    if (fav) favs.remove(s.id) else favs.add(s.id)
                                    prefs.edit().putStringSet("favs", favs.toSet()).apply()
                                }.padding(8.dp)
                            ) {
                                Canvas(Modifier.size(22.dp)) {
                                    val sp2 = starPath(size.width / 2, size.height / 2, size.width / 2)
                                    if (fav) drawPath(sp2, Color(0xFFFFD43B)) else drawPath(sp2, Muted, style = Stroke(2.dp.toPx()))
                                }
                            }
                            Chevron(Blue)
                            Spacer(Modifier.width(4.dp))
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    if (showAbout) AlertDialog(
        onDismissRequest = { showAbout = false },
        containerColor = Color(0xFF0F1438), titleContentColor = Gold, textContentColor = Color.White,
        title = { Text("PingoDNS") },
        text = {
            Column {
                Text("برنامه تغییر DNS مخصوص گیمینگ.\nفقط ترافیک DNS از سرور انتخابی عبور می‌کند و بقیه اینترنت شما تغییری نمی‌کند.")
                Spacer(Modifier.height(14.dp))
                Text("ارتباط با ما در تلگرام: @$TELEGRAM_ID", color = Cyan, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { uri.openUri("https://t.me/$TELEGRAM_ID") })
            }
        },
        confirmButton = { Text("باشه", color = Cyan, modifier = Modifier.clickable { showAbout = false }.padding(12.dp)) }
    )
}
