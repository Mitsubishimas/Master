plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.mastermitsu.cvt"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mastermitsu.cvt"
        minSdk = 21
        targetSdk = 34
        versionCode = 9
        versionName = "2.10.1"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("master.keystore")
            storePassword = "master123"
            keyAlias = "master"
            keyPassword = "master123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    lint {
        disable.add("DuplicatePlatformClasses")
        checkReleaseBuilds = false
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
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
