plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.bajajfinance.beaconconsent"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bajajfinance.beaconconsent"
        minSdk = 24          // Android 7.0 — covers ~99% of active Android devices
        targetSdk = 34       // Android 14
        versionCode = 3
        versionName = "1.0.8-poc"
        vectorDrawables { useSupportLibrary = true }

        // ---- POC CONFIG: point this at your EasyReach / BFL registration endpoint ----
        buildConfigField("String", "SERVER_BASE_URL", "\"https://beacon-poc.smartstation.in\"")
        buildConfigField("String", "POC_API_KEY", "\"REPLACE_WITH_POC_KEY\"")
        buildConfigField("boolean", "DEMO_MODE", "false")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            // Debug = self-contained phone test: simulate the server so the whole flow
            // works on a real phone with no backend. Real paired-device reading still works.
            buildConfigField("boolean", "DEMO_MODE", "true")
            buildConfigField("String", "SERVER_BASE_URL", "\"http://10.0.2.2:8000\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")  // collectAsStateWithLifecycle
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
