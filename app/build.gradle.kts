import java.security.KeyStore
import java.security.MessageDigest

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val buildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
val ksFile = file("release.p12")
val ksPass = System.getenv("KS_PASS") ?: ""
val ksAlias = System.getenv("KS_ALIAS") ?: ""
val hasKey = ksFile.exists() && ksPass.isNotEmpty() && ksAlias.isNotEmpty()

fun certHash(): String {
    if (!hasKey) return ""
    val ks = KeyStore.getInstance("PKCS12")
    ksFile.inputStream().use { ks.load(it, ksPass.toCharArray()) }
    val enc = ks.getCertificate(ksAlias).encoded
    return MessageDigest.getInstance("SHA-256").digest(enc).joinToString("") { "%02x".format(it) }
}

android {
    namespace = "com.neon.dns"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.neon.dns"
        minSdk = 24
        targetSdk = 34
        versionCode = buildNumber
        versionName = "1.$buildNumber"
        buildConfigField("String", "SIG", "\"\"")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    signingConfigs {
        if (hasKey) {
            create("rel") {
                storeFile = ksFile
                storePassword = ksPass
                keyAlias = ksAlias
                keyPassword = ksPass
                storeType = "pkcs12"
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = if (hasKey) signingConfigs.getByName("rel") else signingConfigs.getByName("debug")
            buildConfigField("String", "SIG", "\"${certHash()}\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
}
