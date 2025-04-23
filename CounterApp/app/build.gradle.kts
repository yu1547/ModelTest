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
    implementation("androidx.exifinterface:exifinterface:1.4.0")
    implementation("androidx.core:core:1.12.0")
//    implementation("org.pytorch:pytorch_android:1.9.0")
//    implementation("org.pytorch:pytorch_android_torchvision:1.9.0")
    // ✅ 單元測試依賴（JVM 測試，不需要 Android API）
    testImplementation("junit:junit:4.13.2")

}
