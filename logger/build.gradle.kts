plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    namespace = "com.github.libretube.logger"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
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
    implementation(libs.androidx.core)
    implementation(libs.kotlinx.datetime)
}
