plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "com.example.myapplication22"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myapplication22"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
}

dependencies {
    implementation(libs.mapbox.maps)
    implementation("org.apache.commons:commons-lang3:3.12.0") // StopWatch
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.location)
}