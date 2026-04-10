plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:2.3.12")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.12")
    implementation("io.ktor:ktor-server-websockets-jvm:2.3.12")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.12")
    implementation("ch.qos.logback:logback-classic:1.5.6")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host-jvm:2.3.12")
}

application {
    mainClass.set("com.securechat.signaling.SignalingServerKt")
}

kotlin {
    jvmToolchain(17)
}

// Keep signaling-server source Kotlin-only; generated files are excluded.
val verifyKotlinOnlySource by tasks.registering {
    doLast {
        val sourceDirs = listOf("src/main/kotlin", "src/test/kotlin", "src/main/java", "src/test/java")
            .map { file(it) }
            .filter { it.exists() }

        val javaFiles = sourceDirs.flatMap { dir ->
            fileTree(dir) { include("**/*.java") }.files
        }

        if (javaFiles.isNotEmpty()) {
            val offenders = javaFiles.joinToString(separator = "\n") { it.relativeTo(projectDir).path }
            throw GradleException("Java source files are not allowed in signaling-server source sets:\n$offenders")
        }
    }
}

tasks.named("classes") {
    dependsOn(verifyKotlinOnlySource)
}

