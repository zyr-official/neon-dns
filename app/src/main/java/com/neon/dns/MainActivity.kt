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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

const val TELEGRAM_URL = "https://t.me/pingo_shops"

val Pink = Color(0xFFE41BFF)
val Violet = Color(0xFF7B2FFF)
val Blue = Color(0xFF2F7BFF)
val Cyan = Color(0xFF00E5FF)
val Muted = Color(0xFF9AA8D0)
val Green = Color(0xFF00FF7F)
val Red = Color(0xFFFF2D55)
val CardBg = Color(0xB3080418)

val gradBrush = Brush.horizontalGradient(listOf(Pink, Violet, Blue, Cyan))
val softBrush = Brush.horizontalGradient(listOf(Color(0x887B2FFF), Color(0xAA2F7BFF)))
val dimBrush = Brush.horizontalGradient(listOf(Color(0x557B2FFF), Color(0x552F7BFF)))
val titleBrush = Brush.horizontalGradient(listOf(Pink, Violet, Blue, Cyan))
val ringBrush = Brush.sweepGradient(listOf(Pink, Violet, Blue, Cyan, Blue, Violet, Pink))
val ringThin = Brush.sweepGradient(listOf(Color(0x00E41BFF), Color(0xAA7B2FFF), Color(0x002F7BFF), Color(0xAA00E5FF), Color(0x00E41BFF)))
val faintRing = Brush.sweepGradient(listOf(Color(0x40E41BFF), Color(0x402F7BFF), Color(0x4000E5FF), Color(0x40E41BFF)))
val glowBrush = Brush.radialGradient(listOf(Color(0x557B2FFF), Color.Transparent))
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { App() } }
    }
}

@Composable
fun FaText(t: String, c: Color, size: TextUnit, w: FontWeight = FontWeight.Normal, align: TextAlign = TextAlign.Center, lines: Int = 1) {
    Text(t, color = c, fontSize = size, fontWeight = w, textAlign = align, maxLines = lines,
        overflow = TextOverflow.Ellipsis, style = TextStyle(textDirection = TextDirection.Rtl))
}

@Composable
fun Badge(s: Srv, icon: ImageBitmap?, size: Dp) {
    val shape = RoundedCornerShape(size * 0.24f)
    if (icon != null) {
        Image(
            bitmap = icon, contentDescription = null, contentScale = ContentScale.Crop,
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
fun Chevron(col: Color, right: Boolean, w: Dp = 14.dp, h: Dp = 20.dp) {
    Canvas(Modifier.size(w, h)) {
        val a = if (right) 0.2f else 0.8f
        val b = if (right) 0.8f else 0.2f
        val p = Path().apply {
            moveTo(size.width * a, size.height * .1f)
            lineTo(size.width * b, size.height * .5f)
            lineTo(size.width * a, size.height * .9f)
        }
        drawPath(p, col, style = Stroke(size.width * 0.17f, cap = StrokeCap.Round))
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
fun TelegramIcon(w: Dp = 40.dp, h: Dp = 40.dp) {
    Canvas(Modifier.size(w, h)) {
        val pw = size.width
        val ph = size.height
        val p = Path().apply {
            moveTo(.04f * pw, .46f * ph); lineTo(.96f * pw, .08f * ph)
            lineTo(.66f * pw, .92f * ph); lineTo(.47f * pw, .60f * ph); close()
        }
        drawPath(p, Brush.linearGradient(listOf(Cyan, Blue)))
    }
}

// صفحه اصلی: همه چیز با مختصات دقیق طرح (۸۳۹ × ۱۸۷۵) چیده می‌شه
@Composable
fun MainScreen(
    on: Boolean, selName: String, version: String, rot: State<Float>, pulse: State<Float>,
    onToggle: () -> Unit, onOpenSheet: () -> Unit, onTelegram: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val dens = LocalDensity.current
        val bw = constraints.maxWidth.toFloat()
        val bh = constraints.maxHeight.toFloat()
        val s = maxOf(bw / 839f, bh / 1875f)
        val ox = (bw - 839f * s) / 2f
        val oy = (bh - 1875f * s) / 2f

        fun dd(v: Float): Dp = with(dens) { (v * s).toDp() }
        fun ss(v: Float): TextUnit = with(dens) { dd(v).toSp() }
        fun Modifier.at(x: Float, y: Float, w: Float, h: Float): Modifier =
            this.offset { IntOffset((ox + x * s).roundToInt(), (oy + y * s).roundToInt()) }
                .size(dd(w), dd(h))

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(Modifier.fillMaxSize()) {
                val pill = RoundedCornerShape(50)

                // عنوان و نسخه
                Box(Modifier.at(57f, 36f, 420f, 68f), contentAlignment = Alignment.CenterStart) {
                    Text("PINGODNS", maxLines = 1,
                        style = TextStyle(brush = titleBrush, fontSize = ss(58f), fontWeight = FontWeight.Black))
                }
                Box(Modifier.at(612f, 47f, 170f, 46f).clip(pill).border(dd(2f), gradBrush, pill),
                    contentAlignment = Alignment.Center) {
                    Text("v$version", color = Cyan, fontSize = ss(24f), fontWeight = FontWeight.Bold, maxLines = 1)
                }

                // کارت وضعیت
                val stShape = RoundedCornerShape(dd(62f))
                Box(Modifier.at(200f, 197f, 440f, 141f).clip(stShape).background(CardBg).border(dd(3f), softBrush, stShape))
                Box(Modifier.at(269f, 225f, 26f, 26f).background(if (on) Green else Violet, CircleShape))
                Box(Modifier.at(540f, 224f, 28f, 28f).background(if (on) Green else Red, CircleShape))
                Box(Modifier.at(300f, 210f, 240f, 56f), contentAlignment = Alignment.Center) {
                    FaText("وضعیت اتصال", Color.White, ss(31f))
                }
                Box(Modifier.at(210f, 262f, 420f, 62f), contentAlignment = Alignment.Center) {
                    FaText(if (on) "متصل شد" else "متصل نیست", if (on) Green else Pink, ss(42f), FontWeight.Bold)
                }

                // دکمه پاور
                Box(Modifier.at(90f, 482f, 660f, 660f)
                    .graphicsLayer {
                        val k = if (on) pulse.value else 1f
                        scaleX = k; scaleY = k
                        alpha = if (on) 1f else 0.5f
                    }
                    .background(glowBrush, CircleShape))
                Box(Modifier.at(115f, 507f, 610f, 610f).graphicsLayer { rotationZ = -rot.value }
                    .border(dd(2f), ringThin, CircleShape))
                Box(Modifier.at(135f, 527f, 570f, 570f).border(dd(2f), faintRing, CircleShape))
                Box(Modifier.at(158f, 550f, 524f, 524f).graphicsLayer { rotationZ = rot.value }
                    .border(dd(42f), ringBrush, CircleShape))
                Box(Modifier.at(200f, 592f, 440f, 440f).clip(CircleShape).background(innerBrush)
                    .border(dd(4f), Color(0x552F2A7A), CircleShape))
                Box(Modifier.at(322f, 714f, 196f, 196f), contentAlignment = Alignment.Center) {
                    PowerIcon(Color.White, dd(196f))
                }

                // اتصال به DNS
                Box(Modifier.at(95f, 1219f, 137f, 3f).background(Brush.horizontalGradient(listOf(Color.Transparent, Cyan))))
                Box(Modifier.at(606f, 1219f, 170f, 3f).background(Brush.horizontalGradient(listOf(Cyan, Color.Transparent))))
                Box(Modifier.at(235f, 1180f, 370f, 80f), contentAlignment = Alignment.Center) {
                    FaText(if (on) "قطع اتصال DNS" else "اتصال به DNS", Color.White, ss(48f), FontWeight.Medium)
                }

                // کارت سرور انتخاب شده
                val cardShape = RoundedCornerShape(dd(74f))
                Box(Modifier.at(70f, 1295f, 702f, 148f).clip(cardShape).background(CardBg).border(dd(3f), gradBrush, cardShape))
                Box(Modifier.at(658f, 1327f, 84f, 84f).background(Color(0xFF0E0A22), CircleShape),
                    contentAlignment = Alignment.Center) {
                    Chevron(Color.White, true, dd(18f), dd(30f))
                }
                Box(Modifier.at(322f, 1318f, 306f, 102f), contentAlignment = Alignment.CenterEnd) {
                    Column(horizontalAlignment = Alignment.End) {
                        FaText("سرور انتخاب شده", Muted, ss(27f), align = TextAlign.End)
                        FaText(selName, Color.White, ss(34f), FontWeight.Bold, TextAlign.End, 2)
                    }
                }
                Box(Modifier.at(102f, 1330f, 210f, 78f).clip(pill)
                    .background(Brush.horizontalGradient(listOf(Pink, Blue, Cyan))),
                    contentAlignment = Alignment.Center) {
                    FaText("انتخاب سرور", Color.White, ss(31f), FontWeight.Bold)
                }

                // کارت تلگرام
                val tShape = RoundedCornerShape(dd(66f))
                Box(Modifier.at(96f, 1517f, 646f, 133f).clip(tShape).background(CardBg).border(dd(3f), gradBrush, tShape))
                Box(Modifier.at(136f, 1548f, 84f, 72f), contentAlignment = Alignment.Center) {
                    TelegramIcon(dd(84f), dd(72f))
                }
                Box(Modifier.at(678f, 1560f, 30f, 46f), contentAlignment = Alignment.Center) {
                    Chevron(Color.White, true, dd(20f), dd(34f))
                }
                Box(Modifier.at(235f, 1532f, 430f, 102f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FaText("کانال تلگرامی جهت ارتباط", Muted, ss(28f))
                        Text(TELEGRAM_URL, color = Color.White, fontSize = ss(33f), fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                // لایه‌های لمسی (آخر که روی بقیه باشن)
                Box(Modifier.at(200f, 592f, 440f, 440f).clip(CircleShape).clickable { onToggle() })
                Box(Modifier.at(70f, 1295f, 702f, 148f).clip(cardShape).clickable { onOpenSheet() })
                Box(Modifier.at(96f, 1517f, 646f, 133f).clip(tShape).clickable { onTelegram() })
            }
        }
    }
}

@Composable
fun ServerRow(s: Srv, icon: ImageBitmap?, selected: Boolean, fav: Boolean, onSelect: () -> Unit, onFav: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(shape)
            .background(if (selected) Color(0x2200E5FF) else Color(0xFF0A0F2C))
            .border(if (selected) 2.dp else 1.dp, if (selected) gradBrush else dimBrush, shape)
            .clickable(onClick = onSelect).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Badge(s, icon, 54.dp)
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
    val version = remember {
        try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "" } catch (e: Exception) { "" }
    }
    var sel by remember { mutableStateOf(servers.firstOrNull { it.id == prefs.getString("sel", "") } ?: servers[0]) }
    val favs = remember { mutableStateListOf<String>().apply { addAll(prefs.getStringSet("favs", emptySet())!!) } }
    var query by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("همه") }
    var showSheet by remember { mutableStateOf(false) }
    val on = VpnState.connected

    // پس‌زمینه و آیکون بازی‌ها پشت صحنه آماده می‌شن
    var bg by remember { mutableStateOf<ImageBitmap?>(null) }
    val icons = remember { mutableStateMapOf<String, ImageBitmap>() }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            val bgId = ctx.resources.getIdentifier("bg_main", "drawable", ctx.packageName)
            if (bgId != 0) {
                try { bg = ImageBitmap.imageResource(ctx.resources, bgId) } catch (e: Exception) { }
            }
            servers.map { it.icon }.distinct().forEach { name ->
                val id = ctx.resources.getIdentifier("g_$name", "drawable", ctx.packageName)
                if (id != 0) {
                    try { icons[name] = ImageBitmap.imageResource(ctx.resources, id) } catch (e: Exception) { }
                }
            }
        }
    }

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

    // وقتی صفحه انتخاب سرور بازه، انیمیشن‌های پشتش متوقف می‌شن تا لگ نکنه
    val rotA = remember { Animatable(0f) }
    val pulseA = remember { Animatable(1f) }
    LaunchedEffect(showSheet) {
        if (!showSheet) {
            while (true) {
                rotA.animateTo(rotA.value + 360f, tween(10000, easing = LinearEasing))
            }
        }
    }
    LaunchedEffect(showSheet, on) {
        if (on && !showSheet) {
            while (true) {
                pulseA.animateTo(1.04f, tween(1600, easing = FastOutSlowInEasing))
                pulseA.animateTo(0.96f, tween(1600, easing = FastOutSlowInEasing))
            }
        }
    }
    val rot = rotA.asState()
    val pulse = pulseA.asState()

    val shown by remember {
        derivedStateOf {
            val q = query.trim().lowercase()
            servers.filter { s ->
                (cat == "همه" || (cat == "علاقه‌مندی‌ها" && s.id in favs) || s.cat == cat) &&
                    (q.isEmpty() || (s.name + " " + s.desc + " " + s.tags).lowercase().contains(q))
            }.sortedByDescending { it.id in favs }
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF02020A))) {
        val b = bg
        if (b != null) {
            Image(bitmap = b, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        MainScreen(on, sel.name.substringBefore(" ("), version, rot, pulse,
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
                            Text("انتخاب سرور DNS", color = Cyan, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                                style = TextStyle(textDirection = TextDirection.Rtl))
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
                            s = s, icon = icons[s.icon], selected = s.id == sel.id, fav = s.id in favs,
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
