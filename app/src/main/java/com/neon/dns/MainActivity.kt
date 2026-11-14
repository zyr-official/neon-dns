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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
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

const val TELEGRAM_URL = "https://t.me/pingo_shops"

val Pink = Color(0xFFE41BFF)
val Violet = Color(0xFF7B2FFF)
val Blue = Color(0xFF2F7BFF)
val Cyan = Color(0xFF00E5FF)
val Muted = Color(0xFF9AA8D0)
val Green = Color(0xFF00FF7F)
val Red = Color(0xFFFF2D55)
val CardBg = Color(0xCC0A0620)

val gradBrush = Brush.horizontalGradient(listOf(Pink, Violet, Blue, Cyan))
val dimBrush = Brush.horizontalGradient(listOf(Color(0x557B2FFF), Color(0x552F7BFF)))
val titleBrush = Brush.horizontalGradient(listOf(Pink, Violet, Blue, Cyan))
val ringBrush = Brush.sweepGradient(listOf(Pink, Violet, Blue, Cyan, Blue, Violet, Pink))
val ringThin = Brush.sweepGradient(listOf(Color(0x00E41BFF), Color(0xAA7B2FFF), Color(0x002F7BFF), Color(0xAA00E5FF), Color(0x00E41BFF)))
val glowBrush = Brush.radialGradient(listOf(Color(0x667B2FFF), Color.Transparent))
val innerBrush = Brush.radialGradient(listOf(Color(0xFF16163F), Color(0xFF05050F)))

data class Srv(
    val id: String, val name: String, val desc: String, val ip: String,
    val cat: String, val tags: String, val icon: String, val ab: String
)

val cats = listOf("همه", "علاقه‌مندی‌ها", "بتل‌رویال", "شوتر", "ورزشی", "استراتژی")

// آی‌پی DNS هر سرور رو می‌تونید همین‌جا عوض کنید
val servers = listOf(
    Srv("pubg1", "سرور پابجی موبایل ۱ (PUBG)", "سرور اصلی اروپا و خاورمیانه", "1.1.1.1", "بتل‌رویال", "pubg پابجی", "pubg", "PG"),
    Srv("pubg2", "سرور پابجی موبایل ۲ (PUBG)", "سرور پشتیبان و ارزان", "8.8.8.8", "بتل‌رویال", "pubg پابجی", "pubg", "PG"),
    Srv("ff", "سرور فری فایر (Free Fire)", "اتصال پایدار بدون پاکت لاست", "9.9.9.9", "بتل‌رویال", "free fire فری فایر", "ff", "FF"),
    Srv("cod", "سرور کالاف دیوتی (Call of Duty)", "ثبات پینگ برای بتل رویال و مولتی", "1.1.1.1", "شوتر", "cod کالاف", "cod", "COD"),
    Srv("so2", "سرور استندوف ۲ (Standoff 2)", "مخصوص رقابت‌های تورنمنت", "10.202.10.10", "شوتر", "standoff استندآف", "so2", "S2"),
    Srv("ef1", "سرور EFootball ۱", "بدون تاخیر و شوت آنلاین", "1.1.1.1", "ورزشی", "efootball ای فوتبال pes", "ef", "EF"),
    Srv("ef2", "سرور EFootball ۲", "مخصوص مسابقات آنلاین", "8.8.8.8", "ورزشی", "efootball ای فوتبال pes", "ef", "EF"),
    Srv("coc1", "سرور کلش آف کلنز ۱", "بدون دیسکانکتی در اتک", "1.1.1.1", "استراتژی", "clash of clans coc کلش", "coc", "CC"),
    Srv("coc2", "سرور کلش آف کلنز ۲", "سرور پشتیبان وار کلن", "8.8.8.8", "استراتژی", "clash of clans coc کلش", "coc", "CC"),
    Srv("cr", "سرور کلش رویال (Clash Royale)", "اتصال پایدار در نبردها", "9.9.9.9", "استراتژی", "clash royale کلش رویال", "cr", "CR"),
)

object VpnState { var connected by mutableStateOf(false) }

fun catColors(c: String): List<Color> = when (c) {
    "شوتر" -> listOf(Color(0xFFFF5470), Color(0xFFFF8A3D))
    "بتل‌رویال" -> listOf(Color(0xFFFFB400), Color(0xFFFF6A00))
    "ورزشی" -> listOf(Color(0xFF00E58F), Color(0xFF0097FF))
    "استراتژی" -> listOf(Pink, Color(0xFF5B5BFF))
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

fun Modifier.neonBg(): Modifier = this.drawWithCache {
    val w = size.width
    val h = size.height
    val glows = listOf(
        Brush.radialGradient(listOf(Color(0x77B000FF), Color.Transparent), Offset(0f, h * 0.04f), w * 0.95f),
        Brush.radialGradient(listOf(Color(0x552F7BFF), Color.Transparent), Offset(w, h * 0.28f), w * 0.8f),
        Brush.radialGradient(listOf(Color(0x66B000FF), Color.Transparent), Offset(0f, h * 0.70f), w * 0.8f),
        Brush.radialGradient(listOf(Color(0x6600B7FF), Color.Transparent), Offset(w, h * 0.98f), w * 0.95f)
    )
    fun wave(x0: Float, y0: Float, c1x: Float, c1y: Float, c2x: Float, c2y: Float, x1: Float, y1: Float) =
        Path().apply {
            moveTo(x0 * w, y0 * h)
            cubicTo(c1x * w, c1y * h, c2x * w, c2y * h, x1 * w, y1 * h)
        }
    val waves = listOf(
        wave(0f, 0.20f, 0.15f, 0.03f, 0.55f, 0.12f, 0.85f, 0f),
        wave(0f, 0.28f, 0.20f, 0.08f, 0.60f, 0.20f, 1f, 0.02f),
        wave(1f, 0.30f, 0.80f, 0.28f, 0.60f, 0.45f, 0.30f, 0.56f),
        wave(0f, 0.63f, 0.30f, 0.60f, 0.70f, 0.72f, 1f, 0.82f),
        wave(0f, 0.96f, 0.35f, 0.88f, 0.70f, 0.98f, 1f, 0.86f)
    )
    val wb = Brush.linearGradient(listOf(Pink, Blue, Cyan), Offset(0f, 0f), Offset(w, h))
    val thin = Stroke(2.dp.toPx())
    val thick = Stroke(14.dp.toPx())
    onDrawBehind {
        drawRect(Color(0xFF03030C))
        glows.forEach { drawRect(it) }
        waves.forEach { p ->
            drawPath(p, wb, alpha = 0.16f, style = thick)
            drawPath(p, wb, alpha = 0.85f, style = thin)
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { App() } }
    }
}

@Composable
fun Badge(s: Srv, iconId: Int, size: Dp) {
    val shape = RoundedCornerShape(size * 0.24f)
    if (iconId != 0) {
        Image(
            painter = painterResource(iconId), contentDescription = null, contentScale = ContentScale.Crop,
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
fun Chevron(col: Color, right: Boolean) {
    Canvas(Modifier.size(14.dp, 20.dp)) {
        val a = if (right) 0.2f else 0.8f
        val b = if (right) 0.8f else 0.2f
        val p = Path().apply {
            moveTo(size.width * a, size.height * .1f)
            lineTo(size.width * b, size.height * .5f)
            lineTo(size.width * a, size.height * .9f)
        }
        drawPath(p, col, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
fun StarIcon(fav: Boolean) {
    Canvas(Modifier.size(22.dp)) {
        val sp2 = starPath(size.width / 2, size.height / 2, size.width / 2)
        if (fav) drawPath(sp2, Color(0xFFFFD43B)) else drawPath(sp2, Muted, style = Stroke(2.dp.toPx()))
    }
}

@Composable
fun TelegramIcon() {
    Canvas(Modifier.size(40.dp)) {
        val w = size.width
        val h = size.height
        val p = Path().apply {
            moveTo(.04f * w, .46f * h); lineTo(.96f * w, .08f * h)
            lineTo(.66f * w, .92f * h); lineTo(.47f * w, .60f * h); close()
        }
        drawPath(p, Brush.linearGradient(listOf(Cyan, Blue)))
    }
}

@Composable
fun MainScreen(
    on: Boolean, selName: String, version: String, rot: State<Float>, pulse: State<Float>,
    onToggle: () -> Unit, onOpenSheet: () -> Unit, onTelegram: () -> Unit
) {
    val pill = RoundedCornerShape(50)
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("PINGODNS", style = TextStyle(brush = titleBrush, fontSize = 30.sp, fontWeight = FontWeight.Black),
                    modifier = Modifier.weight(1f))
                Text("v$version", color = Cyan, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(pill).border(1.dp, gradBrush, pill).padding(horizontal = 12.dp, vertical = 5.dp))
            }
        }

        val st = RoundedCornerShape(30.dp)
        Column(
            Modifier.padding(top = 24.dp).fillMaxWidth(0.74f).clip(st).background(CardBg)
                .border(1.5.dp, gradBrush, st).padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(if (on) Green else Violet, CircleShape))
                Spacer(Modifier.width(14.dp))
                Text("وضعیت اتصال", color = Color.White, fontSize = 15.sp)
                Spacer(Modifier.width(14.dp))
                Box(Modifier.size(10.dp).background(if (on) Green else Red, CircleShape))
            }
            Text(if (on) "متصل شد" else "متصل نیست", color = if (on) Green else Pink,
                fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(30.dp))
        Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(300.dp)
                    .graphicsLayer {
                        val s = if (on) pulse.value else 1f
                        scaleX = s; scaleY = s
                        alpha = if (on) 1f else 0.55f
                    }
                    .background(glowBrush, CircleShape)
            )
            Box(Modifier.size(274.dp).graphicsLayer { rotationZ = -rot.value }.border(1.5.dp, ringThin, CircleShape))
            Box(Modifier.size(244.dp).graphicsLayer { rotationZ = rot.value }.border(18.dp, ringBrush, CircleShape))
            Box(
                Modifier.size(200.dp).clip(CircleShape).background(innerBrush).clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) { PowerIcon(Color.White, 84.dp) }
        }

        Spacer(Modifier.height(10.dp))
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(2.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Cyan))))
                Text(if (on) "قطع اتصال DNS" else "اتصال به DNS", color = Color.White, fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 14.dp))
                Box(Modifier.weight(1f).height(2.dp).background(Brush.horizontalGradient(listOf(Cyan, Color.Transparent))))
            }
        }

        val cs = RoundedCornerShape(40.dp)
        Row(
            Modifier.fillMaxWidth().padding(top = 26.dp).clip(cs).background(CardBg).border(1.5.dp, gradBrush, cs)
                .clickable { onOpenSheet() }.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(46.dp).background(Color(0xFF0E0A22), CircleShape), contentAlignment = Alignment.Center) {
                Chevron(Color.White, true)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("سرور انتخاب شده", color = Muted, fontSize = 13.sp)
                Text(selName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1)
            }
            Text("انتخاب سرور", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                modifier = Modifier.clip(pill)
                    .background(Brush.horizontalGradient(listOf(Pink, Blue, Cyan)))
                    .padding(horizontal = 18.dp, vertical = 11.dp))
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp).clip(cs).background(CardBg).border(1.5.dp, gradBrush, cs)
                .clickable { onTelegram() }.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Chevron(Color.White, true)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("کانال تلگرامی جهت ارتباط", color = Muted, fontSize = 13.sp)
                Text(TELEGRAM_URL, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
            }
            TelegramIcon()
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
fun ServerRow(s: Srv, iconId: Int, selected: Boolean, fav: Boolean, onSelect: () -> Unit, onFav: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(shape)
            .background(if (selected) Color(0x2200E5FF) else Color(0xFF0A0F2C))
            .border(if (selected) 2.dp else 1.dp, if (selected) gradBrush else dimBrush, shape)
            .clickable(onClick = onSelect).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Badge(s, iconId, 54.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(s.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2)
            Text(s.desc, color = Muted, fontSize = 13.sp, maxLines = 1)
        }
        Box(Modifier.clip(CircleShape).clickable(onClick = onFav).padding(8.dp)) { StarIcon(fav) }
        Chevron(Blue, false)
        Spacer(Modifier.width(4.dp))
    }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    val uri = LocalUriHandler.current
    val prefs = remember { ctx.getSharedPreferences("neon", 0) }
    val iconIds = remember {
        servers.associate { s ->
            s.id to ctx.resources.getIdentifier("g_" + s.icon, "drawable", ctx.packageName)
        }
    }
    val version = remember {
        try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "" } catch (e: Exception) { "" }
    }
    var sel by remember { mutableStateOf(servers.firstOrNull { it.id == prefs.getString("sel", "") } ?: servers[0]) }
    val favs = remember { mutableStateListOf<String>().apply { addAll(prefs.getStringSet("favs", emptySet())!!) } }
    var query by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("همه") }
    var showSheet by remember { mutableStateOf(false) }
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
    BackHandler(enabled = showSheet) { showSheet = false }

    val t = rememberInfiniteTransition(label = "t")
    val rot = t.animateFloat(0f, 360f, infiniteRepeatable(tween(10000, easing = LinearEasing)), label = "r")
    val pulse = t.animateFloat(0.96f, 1.04f,
        infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")

    val shown by remember {
        derivedStateOf {
            val q = query.trim().lowercase()
            servers.filter { s ->
                (cat == "همه" || (cat == "علاقه‌مندی‌ها" && s.id in favs) || s.cat == cat) &&
                    (q.isEmpty() || (s.name + " " + s.desc + " " + s.tags).lowercase().contains(q))
            }.sortedByDescending { it.id in favs }
        }
    }

    Box(Modifier.fillMaxSize().neonBg()) {
        MainScreen(on, sel.name, version, rot, pulse,
            onToggle = { toggle() }, onOpenSheet = { showSheet = true }, onTelegram = { uri.openUri(TELEGRAM_URL) })

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
                    .border(2.dp, gradBrush, sheetShape)
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
                            Modifier.size(52.dp).clip(CircleShape).border(1.5.dp, Violet, CircleShape)
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
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Color(0xFF1A2145))
                                .padding(horizontal = 18.dp, vertical = 16.dp),
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
                        Text(c, color = if (act) Color(0xFF050B1A) else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(50))
                                .background(if (act) Brush.horizontalGradient(listOf(Cyan, Color(0xFF7AF0FF))) else Brush.horizontalGradient(listOf(Color(0x14FFFFFF), Color(0x14FFFFFF))))
                                .clickable { cat = c }.padding(horizontal = 15.dp, vertical = 7.dp))
                    }
                }
                LazyColumn(Modifier.weight(1f).padding(top = 6.dp)) {
                    if (shown.isEmpty()) item {
                        Text("موردی پیدا نشد", color = Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp))
                    }
                    items(shown, key = { it.id }) { s ->
                        ServerRow(
                            s = s, iconId = iconIds[s.id] ?: 0, selected = s.id == sel.id, fav = s.id in favs,
                            onSelect = {
                                sel = s
                                prefs.edit().putString("sel", s.id).apply()
                                if (VpnState.connected) start(s.ip)
                                showSheet = false
                            },
                            onFav = {
                                if (s.id in favs) favs.remove(s.id) else favs.add(s.id)
                                prefs.edit().putStringSet("favs", favs.toSet()).apply()
                            }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}
