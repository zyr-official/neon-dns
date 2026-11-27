package com.neon.dns

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val STORE_APP = "myket://details?id=com.neon.dns"
private const val STORE_WEB = "https://myket.ir/app/com.neon.dns"

fun openStore(ctx: Context) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(STORE_APP)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(STORE_WEB)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e2: Exception) {
        }
    }
}

// صفحه بروزرسانی اجباری: وقتی نسخه نصب‌شده از min کمتر باشه روی کل اپ میاد
@Composable
fun UpdateGate() {
    val need = RemoteConfig.needsUpdate()
    if (!need) return
    val ctx = LocalContext.current

    // تا وقتی صفحه بازه، هر ۲۰ ثانیه دوباره چک می‌کنه (اگه min کم بشه خودش بسته می‌شه)
    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000)
            RemoteConfig.refresh(ctx, true)
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color(0xF2020208))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "نسخه جدید آماده است", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, style = TextStyle(textDirection = TextDirection.Rtl)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "نسخه‌ای که نصب کرده‌اید قدیمی است. برای ادامه، برنامه را از مایکت بروزرسانی کنید.",
                color = Color(0xFF9AA8D0), fontSize = 15.sp, textAlign = TextAlign.Center,
                style = TextStyle(textDirection = TextDirection.Rtl)
            )
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFFE41BFF), Color(0xFF2F7BFF), Color(0xFF00E5FF))),
                        RoundedCornerShape(50)
                    )
                    .clickable { openStore(ctx) }
                    .padding(horizontal = 32.dp, vertical = 14.dp)
            ) {
                Text(
                    "بروزرسانی از مایکت", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    style = TextStyle(textDirection = TextDirection.Rtl)
                )
            }
        }
    }
}
