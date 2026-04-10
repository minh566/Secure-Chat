// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false

    // THÊM DÒNG NÀY ĐỂ FIX LỖI PLUGIN CHO SIGNALING-SERVER
    kotlin("jvm") version "2.1.0" apply false
}