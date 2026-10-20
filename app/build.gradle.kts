plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.epicmems.stride"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.epicmems.stride"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    /**
     * One stable signing key for every build.
     *
     * This is what makes a new APK install *over* the previous one. GitHub's runners are wiped
     * between runs, so without this each build generated a brand-new debug key and Android
     * rejected the update with a signature mismatch — forcing an uninstall every time.
     *
     * The key comes from CI as the STRIDE_KEYSTORE_* environment variables (repository
     * secrets). When they are absent — a normal local build — AGP falls back to the default
     * debug keystore, so `./gradlew assembleDebug` still works on its own.
     */
    signingConfigs {
        create("stride") {
            val path = System.getenv("STRIDE_KEYSTORE_PATH")
            if (!path.isNullOrBlank() && file(path).exists()) {
                storeFile = file(path)
                storeType = "PKCS12"
                storePassword = System.getenv("STRIDE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("STRIDE_KEY_ALIAS")
                keyPassword = System.getenv("STRIDE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            signingConfigs.findByName("stride")
                ?.takeIf { it.storeFile != null }
                ?.let { signingConfig = it }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("stride")
                ?.takeIf { it.storeFile != null }
                ?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        // Print every issue to the build log, not just the first one — CI is our compiler.
        textReport = true
        textOutput = file("stdout")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
