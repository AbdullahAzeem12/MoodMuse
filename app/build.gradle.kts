import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.moodmuse"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.moodmuse"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProps = Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localPropsFile.inputStream().use { localProps.load(it) }
        }

        fun sanitizeSecret(v: String?): String {
            val raw = v?.trim() ?: ""
            if (raw.length >= 2 && ((raw.startsWith("\"") && raw.endsWith("\"")) || (raw.startsWith("'") && raw.endsWith("'")))) {
                return raw.substring(1, raw.length - 1)
            }
            return raw
        }

        val jamendoClientId = sanitizeSecret(
            localProps.getProperty("JAMENDO_CLIENT_ID")
                ?: localProps.getProperty("jamendo_client_id")
                ?: providers.gradleProperty("JAMENDO_CLIENT_ID").orNull
                ?: ""
        )

        val jamendoClientSecret = sanitizeSecret(
            localProps.getProperty("JAMENDO_CLIENT_SECRET")
                ?: localProps.getProperty("jamendo_client_secret")
                ?: providers.gradleProperty("JAMENDO_CLIENT_SECRET").orNull
                ?: ""
        )
        buildConfigField("String", "JAMENDO_CLIENT_ID", "\"$jamendoClientId\"")
        buildConfigField("String", "JAMENDO_CLIENT_SECRET", "\"$jamendoClientSecret\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        mlModelBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.viewpager2)
    implementation(libs.glide)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // Security & Biometrics
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // CameraX for camera preview
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // Firebase ML Kit for face detection
    implementation("com.google.mlkit:face-detection:16.1.7")

    // TensorFlow Lite for advanced emotion recognition
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.1.0")

    // Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // ExoPlayer for native music playback (Replaces Spotify SDK)
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")

    // UI Animations
    implementation("com.airbnb.android:lottie:6.4.0")

    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Room Database for History
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")

    // Testing
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}
