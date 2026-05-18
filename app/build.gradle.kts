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
        versionCode = 1816
        versionName = "1816-default-5s"
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
