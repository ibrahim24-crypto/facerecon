plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val releaseStoreFile = rootProject.file("key")
val releaseStorePassword = providers.gradleProperty("facereconReleaseStorePassword")
    .orElse(providers.environmentVariable("FACERECON_RELEASE_STORE_PASSWORD"))
    .orNull
val releaseKeyAlias = providers.gradleProperty("facereconReleaseKeyAlias")
    .orElse(providers.environmentVariable("FACERECON_RELEASE_KEY_ALIAS"))
    .orNull
val releaseKeyPassword = providers.gradleProperty("facereconReleaseKeyPassword")
    .orElse(providers.environmentVariable("FACERECON_RELEASE_KEY_PASSWORD"))
    .orNull

android {
    namespace = "com.himo.facerecon"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.himo.facerecon"
        // Support Android 8.1 (API 27) and above as requested
        minSdk = 27
        // Keep targetSdk equal to compileSdk for best compatibility with latest OS behavior
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = releaseStoreFile
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

tasks.configureEach {
    if (name == "validateSigningRelease") {
        doFirst {
            val missing = buildList {
                if (!releaseStoreFile.isFile) add("root/key signing file")
                if (releaseStorePassword.isNullOrBlank()) add("FACERECON_RELEASE_STORE_PASSWORD")
                if (releaseKeyAlias.isNullOrBlank()) add("FACERECON_RELEASE_KEY_ALIAS")
                if (releaseKeyPassword.isNullOrBlank()) add("FACERECON_RELEASE_KEY_PASSWORD")
            }
            if (missing.isNotEmpty()) {
                throw GradleException(
                    "Release signing is not configured. Missing: ${missing.joinToString()}. " +
                        "Set these locally via Gradle properties or environment variables; never commit them."
                )
            }
        }
    }
}

kotlin {
    jvmToolchain(8)
}

dependencies {
    implementation(libs.androidx.core.ktx)

    // CameraX
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-gpu-api:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-support-api:0.4.4")

    // Lifecycle & LiveData
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.activity.compose)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
