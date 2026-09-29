plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.nadaworks.watchnavigation"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.nadaworks.watchnavigation"
        minSdk = 30
        targetSdk = 36
        versionCode = 5
        versionName = "0.3.1"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.wear.compose:compose-material3:1.7.0")
    testImplementation("junit:junit:4.13.2")
}
