plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.securechat"
    compileSdk = 36

    flavorDimensions += "env"

    defaultConfig {
        applicationId = "com.securechat"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    productFlavors {
        create("dev") {
            dimension = "env"
            buildConfigField("String", "SIGNALING_HTTP_URL", "\"http://10.0.2.2:8081\"")
            buildConfigField("String", "SIGNALING_WS_URL", "\"ws://10.0.2.2:8081/ws\"")
        }

        create("prod") {
            dimension = "env"
            buildConfigField("String", "SIGNALING_HTTP_URL", "\"https://signal.securechat.example.com\"")
            buildConfigField("String", "SIGNALING_WS_URL", "\"wss://signal.securechat.example.com/ws\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Keep authored app sources Kotlin-only; generated sources under /build are excluded.
val verifyKotlinOnlySource by tasks.registering {
    doLast {
        val sourceDirs = listOf("src/main/java", "src/main/kotlin", "src/debug/java", "src/debug/kotlin")
            .map { file(it) }
            .filter { it.exists() }

        val javaFiles = sourceDirs.flatMap { dir ->
            fileTree(dir) { include("**/*.java") }.files
        }

        if (javaFiles.isNotEmpty()) {
            val offenders = javaFiles.joinToString(separator = "\n") { it.relativeTo(projectDir).path }
            throw GradleException("Java source files are not allowed in app source sets:\n$offenders")
        }
    }
}

tasks.named("preBuild") {
    dependsOn(verifyKotlinOnlySource)
}

dependencies {
    // ── Jetpack Compose BOM ──────────────────────────────────────
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    // ── Core Android ─────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.messaging.ktx)

    // ── Room (offline cache) ─────────────────────────────────────
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ── WebRTC ───────────────────────────────────────────────────
    implementation(libs.webrtc)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // ── Coroutines ───────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // ── Image loading ────────────────────────────────────────────
    implementation(libs.coil.compose)

    // ── DataStore (settings) ─────────────────────────────────────
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.biometric)

    // ── Unit tests ───────────────────────────────────────────────
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
