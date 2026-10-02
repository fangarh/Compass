plugins {
    id("com.android.application")
}

android {
    namespace = "net.afterday.compas"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "net.afterday.compas"
        minSdk = 23
        targetSdk = 35
        versionCode = 1853
        versionName = "1853-wifi-target-rssi-averaging"
    }

    flavorDimensions += "mainActions"

    productFlavors {
        create("standard") {
            dimension = "mainActions"
            buildConfigField("boolean", "SHOW_MAIN_ACTION_BUTTONS", "true")
        }
        create("legacy") {
            dimension = "mainActions"
            minSdk = 18
            versionNameSuffix = "-android4-5-hidden-iff"
            buildConfigField("boolean", "SHOW_MAIN_ACTION_BUTTONS", "false")
        }
        create("hiddenMainActions") {
            dimension = "mainActions"
            applicationIdSuffix = ".hidden"
            buildConfigField("boolean", "SHOW_MAIN_ACTION_BUTTONS", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("com.android.support:appcompat-v7:28.0.0") {
        exclude(group = "com.android.support", module = "animated-vector-drawable")
    }
    implementation("com.android.support:recyclerview-v7:28.0.0")
    implementation("com.android.support:support-v4:28.0.0")
    implementation("com.android.support:percent:28.0.0")
    implementation("com.android.support.constraint:constraint-layout:1.1.3")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("com.journeyapps:zxing-android-embedded:3.6.0")
    implementation("com.google.zxing:core:3.3.3")
    implementation("net.sourceforge.streamsupport:streamsupport:1.7.4")
}

tasks.register<Copy>("packageLegacyDebugApk") {
    dependsOn("assembleLegacyDebug")
    from(layout.buildDirectory.file("outputs/apk/legacy/debug/app-legacy-debug.apk"))
    into(rootProject.layout.projectDirectory.dir("artifacts/android-4-5"))
    rename { "compass-android-4-5-hidden-iff-debug.apk" }
}
