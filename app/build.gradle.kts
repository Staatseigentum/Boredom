plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/**
 * The release workflow passes the tag in, so a tag of `v1.2.0` produces exactly that version
 * name. Without it we fall back to the development version.
 */
val appVersionName: String =
    System.getenv("KOLLAPS_VERSION_NAME")?.removePrefix("v")?.takeIf { it.isNotBlank() } ?: "1.0"

/**
 * Android only treats a build as an update when the code goes up, so it is derived from the
 * version name instead of being maintained by hand: 1.2.3 becomes 10203.
 */
val appVersionCode: Int = run {
    val parts = appVersionName
        .takeWhile { it.isDigit() || it == '.' }
        .split('.')
        .mapNotNull { it.toIntOrNull() }
    val major = parts.getOrElse(0) { 0 }
    val minor = parts.getOrElse(1) { 0 }
    val patch = parts.getOrElse(2) { 0 }
    (major * 10_000 + minor * 100 + patch).coerceAtLeast(1)
}

// Release signing comes from the environment so no key material lives in the repository.
val keystorePath: String? = System.getenv("KOLLAPS_KEYSTORE")
val keystorePassword: String? = System.getenv("KOLLAPS_KEYSTORE_PASSWORD")
val keystoreAlias: String? = System.getenv("KOLLAPS_KEY_ALIAS")
val keystoreAliasPassword: String? = System.getenv("KOLLAPS_KEY_PASSWORD")
val hasSigningKey: Boolean = !keystorePath.isNullOrBlank() && file(keystorePath).exists()

android {
    namespace = "com.staatseigentum.kollaps"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.staatseigentum.kollaps"
        minSdk = 24
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (hasSigningKey) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = keystorePassword
                keyAlias = keystoreAlias
                keyPassword = keystoreAliasPassword
            }
        }
    }

    buildTypes {
        release {
            // Absent locally and in the plain CI build; the release workflow supplies it.
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
