
package com.neon.dns

import android.app.Activity
import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.os.Handler
import android.os.Looper
import java.security.MessageDigest

class PingoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!Guard.ok(this)) {
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(0)
        }

        // تنظیمات سرورها: اول آخرین نسخه ذخیره‌شده، بعد بروزرسانی از اینترنت
        RemoteConfig.load(this)
        RemoteConfig.refresh(this)

        // بعد از قطع اتصال، دوباره تنظیمات رو می‌گیره
        val h = Handler(Looper.getMainLooper())
        var was = false
        h.post(object : Runnable {
            override fun run() {
                val now = VpnState.connected
                if (was && !now) RemoteConfig.refresh(this@PingoApp, true, 1500)
                was = now
                h.postDelayed(this, 2000)
            }
        })

        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(a: Activity) {
                a.window.decorView.filterTouchesWhenObscured = true
                RemoteConfig.refresh(a)
            }
            override fun onActivityCreated(a: Activity, b: Bundle?) {}
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityPaused(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })
    }
}

object Guard {
    fun ok(app: Application): Boolean {
        if (BuildConfig.DEBUG) return true
        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) return false
        if (app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) return false
        val expected = BuildConfig.SIG
        if (expected.isNotEmpty()) {
            val got = signerHash(app) ?: return false
            if (!got.equals(expected, ignoreCase = true)) return false
        }
        return true
    }

    @Suppress("DEPRECATION")
    private fun signerHash(app: Application): String? = try {
        val pm = app.packageManager
        val bytes = if (Build.VERSION.SDK_INT >= 28) {
            val si = pm.getPackageInfo(app.packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo
            si?.apkContentsSigners?.firstOrNull()?.toByteArray()
        } else {
            pm.getPackageInfo(app.packageName, PackageManager.GET_SIGNATURES).signatures?.firstOrNull()?.toByteArray()
        }
        if (bytes == null) null
        else MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    } catch (e: Exception) { null }
}
