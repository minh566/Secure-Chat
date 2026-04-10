pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Added for stream-webrtc-android
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "SecureChat"
include(":app")
project(":app").projectDir = file("SecureChat/app")
include(":signaling-server")
project(":signaling-server").projectDir = file("signaling-server")
