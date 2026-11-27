plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.alsaif.mushaf"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.alsaif.mushaf"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "4.0"
    }

    // مفتاح توقيع ثابت: يخلّي كل نسخة جديدة تتثبت فوق القديمة بدون حذف
    signingConfigs {
        create("mushaf") {
            storeFile = file("keystore/mushaf.jks")
            storePassword = "mushaf123"
            keyAlias = "mushaf"
            keyPassword = "mushaf123"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("mushaf")
        }
        release {
            signingConfig = signingConfigs.getByName("mushaf")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("androidx.media3:media3-exoplayer:1.5.1")
}

// تحميل خطوط المصحف تلقائياً أثناء البناء (Amiri Quran و Aref Ruqaa من Google Fonts)
// لو فشل التحميل يكمل البناء عادي ويستخدم التطبيق خط الجوال
val downloadFonts by tasks.registering {
    val dir = file("src/main/assets/fonts")
    doLast {
        dir.mkdirs()
        mapOf(
            "AmiriQuran-Regular.ttf" to listOf(
                "https://mirrors.mit.edu/CTAN/fonts/amiri/AmiriQuran.ttf",
                "https://raw.githubusercontent.com/google/fonts/main/ofl/amiriquran/AmiriQuran-Regular.ttf",
                "https://mirrors.ctan.org/fonts/amiri/AmiriQuran.ttf",
            ),
            "ArefRuqaa-Bold.ttf" to listOf(
                "https://raw.githubusercontent.com/google/fonts/main/ofl/arefruqaa/ArefRuqaa-Bold.ttf",
                "https://cdn.jsdelivr.net/gh/google/fonts@main/ofl/arefruqaa/ArefRuqaa-Bold.ttf",
            ),
        ).forEach { (name, urls) ->
            val target = File(dir, name)
            for (url in urls) {
                if (target.exists() && target.length() > 10_000) break
                try {
                    java.net.URI(url).toURL().openStream().use { input -> target.outputStream().use { input.copyTo(it) } }
                    println("Downloaded font $name from $url (${target.length()} bytes)")
                } catch (e: Exception) {
                    target.delete()
                    println("WARNING: font $name not downloaded from $url: ${e.message}")
                }
            }
        }
    }
}
tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(downloadFonts) }
