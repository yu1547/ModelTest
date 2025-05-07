plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.example.counterapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.counterapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    // 其他主要依賴
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.core)

    implementation("org.pytorch:pytorch_android:1.13.1") // 替換為最新的穩定版本
    implementation("org.pytorch:pytorch_android_torchvision:1.13.1") // 替換為最新的穩定版本

    implementation("com.android.volley:volley:1.2.1")
    // ✅ 單元測試依賴（JVM 測試，不需要 Android API）
    testImplementation(libs.junit)

}
