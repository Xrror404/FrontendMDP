plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
    kotlin("kapt")
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.projectmdp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.projectmdp"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }


}

dependencies {
    // AndroidX Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0") // Keep this one for ViewModel specific to Compose

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom)) // Use the BOM from your libs.versions.toml
    implementation(libs.androidx.activity.compose) // Correctly uses libs.versions.toml
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3) // Using the one from libs.versions.toml
    implementation("androidx.navigation:navigation-compose:2.7.7") // Specific version for navigation
    implementation("androidx.compose.foundation:foundation") // Essential Compose foundation components
    implementation("androidx.compose.runtime:runtime-livedata:1.6.8")
    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.4.0")

    // RoomDB
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1") // Don't forget the KAPT for Room!

    // Retrofit with Moshi
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi:1.15.2")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.2")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.15.2")
    implementation(libs.firebase.auth)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.15.0")) // Firebase BoM
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.android.gms:play-services-auth:20.7.0") // Play Services Auth

    // Hilt (Dagger Hilt)
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0") // Hilt integration for Compose Navigation
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom)) // BOM for Compose testing
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    testImplementation("junit:junit:4.13.2")
// Mockito untuk mocking
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.mockito:mockito-inline:4.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
// Coroutines Testing
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
// Architecture Components Testing (untuk InstantTaskExecutorRule)
    testImplementation("androidx.arch.core:core-testing:2.2.0")
// LiveData Testing
    testImplementation("androidx.lifecycle:lifecycle-runtime-testing:2.7.0")
// Room Testing (jika menggunakan Room database)
    testImplementation("androidx.room:room-testing:2.6.1")
// Truth assertion library (optional tapi recommended)
    testImplementation("com.google.truth:truth:1.1.4")

    // Di build.gradle.kts - untuk WebView jika butuh embedded payment
    implementation("androidx.webkit:webkit:1.8.0")

    // Untuk handle deep links dari Midtrans callback
    implementation("androidx.browser:browser:1.7.0")

    implementation("androidx.compose.material:material-icons-extended:1.6.0")
}
