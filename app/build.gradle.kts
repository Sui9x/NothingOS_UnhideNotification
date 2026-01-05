plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.sui.nothingunhidenotif"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sui.nothingunhidenotif"
        minSdk = 27
        targetSdk = 35
        versionCode = 2
        versionName = "1.2"
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    // Xposed API
    compileOnly("de.robv.android.xposed:api:82")

    // API回避
    compileOnly("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")

    // Material
    implementation("com.google.android.material:material:1.12.0")
}
