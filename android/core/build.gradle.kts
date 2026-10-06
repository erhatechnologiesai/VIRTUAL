plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.virtual.core"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    api(libs.androidx.core.ktx)
    api(libs.retrofit)
    api(libs.retrofit.converter.gson)
    api(libs.okhttp.logging)
    api(libs.socketio.client)
    api(libs.androidx.security.crypto)
    api(libs.gson)
    api(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
}
