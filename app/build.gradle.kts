import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

// Load properties from local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.syed"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.syed"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // NDK configuration for native library
        ndk {
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
            abiFilters.add("x86")
            abiFilters.add("x86_64")
        }

        externalNativeBuild {
            cmake {
                cppFlags += ""
            }
        }

        // Inject Google Maps API key from gradle.properties
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] =
            project.findProperty("GOOGLE_MAPS_API_KEY") as String? ?: "AIzaSyDummyKeyForBuildProcess"

        // Add Google Maps API key to BuildConfig for Places SDK initialization
        buildConfigField(
            "String",
            "GOOGLE_MAPS_API_KEY",
            "\"${project.findProperty("GOOGLE_MAPS_API_KEY") ?: "YOUR_ACTUAL_GOOGLE_MAPS_API_KEY"}\"",
        )

        // API keys are now securely managed through Remote Config + NDK
        // BuildConfig key is only used for Places SDK initialization at app startup
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Additional security for production
            isDebuggable = false
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
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
        viewBinding = true
        dataBinding = false
        buildConfig = true // Enable BuildConfig generation
    }

    // Force resolution strategy to resolve version conflicts
    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.24")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.24")
            force("org.jetbrains.kotlin:kotlin-stdlib-common:1.9.24")
        }

        // Exclude firebase-iid globally to prevent duplicate class error
        // firebase-messaging 23.2.1 includes IID functionality, so the old firebase-iid module is not needed
        exclude(group = "com.google.firebase", module = "firebase-iid")
    }
}

val stblib = "1.9.24"
val recycler = "1.3.1"
val navigtion_fragment_ui = "2.5.3"
val gms_play_services_auth = "20.6.0"
val gms_play_services_location = "21.0.1"
val gms_play_services_maps = "18.1.0"
val firebasebom = "32.2.3"
val glide = "4.16.0"
val swiperfreshlayout = "1.1.0"
val viewpager = "1.0.0"
val dotsindicator = "4.3"
val lifecycle_viewmodel_ktx = "2.6.1"
val lifecycle_livedata_ktx = "2.6.1"
val work_runtime_ktx = "2.8.1"
val gson = "2.10.1"
val coroutine_version = "1.6.4"
val credentials = "1.2.0"
// Advanced feature versions
val room_version = "2.5.2"
val retrofit_version = "2.9.0"
val okhttp_version = "4.11.0"
val mp_android_chart = "v3.1.0"
val lottie_version = "6.1.0"
val coil_version = "2.4.0"
val paging_version = "3.2.0"
val hilt_version = "2.48"
val ml_kit_version = "17.0.0"

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Force Kotlin stdlib version to match compiler
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$stblib")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$stblib")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$stblib")

    // Firebase - use older BOM version compatible with Kotlin 1.9
    implementation(platform("com.google.firebase:firebase-bom:$firebasebom"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-config") // Remote Config for secure key storage
    implementation("com.google.firebase:firebase-config-ktx") // Remote Config KTX extensions
    implementation("com.google.firebase:firebase-common-ktx") // Firebase Common KTX
    implementation("com.google.firebase:firebase-appcheck-playintegrity") // Production App Check
    implementation("com.google.firebase:firebase-appcheck-debug") // Debug App Check (for development)

    // Google Sign In - use compatible version
    implementation("com.google.android.gms:play-services-auth:$gms_play_services_auth")

    // Location and Maps - use compatible versions
    implementation("com.google.android.gms:play-services-location:$gms_play_services_location")
    implementation("com.google.android.gms:play-services-maps:$gms_play_services_maps")
    implementation("com.google.android.libraries.places:places:3.3.0") // Places SDK for location autocomplete and search

    // Navigation - use compatible versions
    implementation("androidx.navigation:navigation-fragment-ktx:$navigtion_fragment_ui")
    implementation("androidx.navigation:navigation-ui-ktx:$navigtion_fragment_ui")

    // Image loading
    implementation("com.github.bumptech.glide:glide:$glide")
    implementation(libs.google.material)
    annotationProcessor("com.github.bumptech.glide:compiler:$glide")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:$recycler")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")
    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:$swiperfreshlayout")

    // ViewPager2
    implementation("androidx.viewpager2:viewpager2:$viewpager")

    // ViewPager Dots Indicator
    implementation("com.tbuonomo:dotsindicator:$dotsindicator")

    implementation("com.google.android.material:material:1.9.0")

    // Lifecycle components - use compatible versions
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_viewmodel_ktx")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_livedata_ktx")

    // Work Manager for background sync
    implementation("androidx.work:work-runtime-ktx:$work_runtime_ktx")

    // Gson for JSON serialization
    implementation("com.google.code.gson:gson:$gson")

    // Coroutines support - use compatible versions
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutine_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:$coroutine_version")

    // Credentials - use compatible versions
    implementation("androidx.credentials:credentials:$credentials")
    implementation("androidx.credentials:credentials-play-services-auth:$credentials")

    // ===== ADVANCED FEATURES =====

    // Room Database for offline caching
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")

    // Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:$retrofit_version")
    implementation("com.squareup.retrofit2:converter-gson:$retrofit_version")
    implementation("com.squareup.okhttp3:okhttp:$okhttp_version")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttp_version")

    // MPAndroidChart for analytics graphs
    implementation("com.github.PhilJay:MPAndroidChart:$mp_android_chart")

    // Lottie for animations
    implementation("com.airbnb.android:lottie:$lottie_version")

    // Coil for advanced image loading
    implementation("io.coil-kt:coil:$coil_version")
    implementation("io.coil-kt:coil-gif:$coil_version")

    // Paging 3 for efficient list loading
    implementation("androidx.paging:paging-runtime-ktx:$paging_version")

    // ML Kit for on-device pet detection (FREE, no network required)
    implementation("com.google.mlkit:image-labeling:17.0.8")
    // Optional: Custom model support for better pet detection
    implementation("com.google.mlkit:linkfirebase:17.0.0")

    // Firebase ML
    implementation("com.google.firebase:firebase-ml-modeldownloader")

    // ExoPlayer for video playback
    implementation("androidx.media3:media3-exoplayer:1.1.1")
    implementation("androidx.media3:media3-ui:1.1.1")

    // DataStore for preferences (better than SharedPreferences)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Shimmer effect for loading states
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    // Material Dialogs
    implementation("com.afollestad.material-dialogs:core:3.3.0")

    // Image Picker
    implementation("com.github.dhaval2404:imagepicker:2.1")

    // Compressor for image compression
    implementation("id.zelory:compressor:3.0.1")
    implementation("com.android.volley:volley:1.2.1")
    implementation("io.coil-kt:coil:2.5.0")

    implementation("com.google.android.material:material:1.10.0")
    implementation("io.coil-kt:coil:2.4.0")

    // Material Components (includes BottomSheetDialog)
    implementation("com.google.android.material:material:1.11.0")
// OR if you want the latest version as of 2024:
    implementation("com.google.android.material:material:1.12.0-alpha02")

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
