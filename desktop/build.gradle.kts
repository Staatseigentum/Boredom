plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    application
}

/**
 * Compose Multiplatform artifacts, pulled in by coordinate rather than through the Compose Gradle
 * plugin: the harness only needs the libraries, not the native packaging the plugin exists for.
 */
private val composeVersion = libs.versions.composeMultiplatform.get()

/**
 * The desktop harness.
 *
 * It runs the real game screen rather than a lookalike: the interface sources are compiled
 * straight out of the app module. That works because Compose Multiplatform publishes the same
 * `androidx.compose.*` API the Android artifacts do, so one set of sources satisfies both — which
 * is the whole point. A mock would drift from the app and stop catching its bugs.
 *
 * Only the genuinely Android-shaped files are left out, listed one by one rather than by pattern
 * so that a new file has to be considered rather than silently skipped.
 */
kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    sourceSets["main"].kotlin {
        srcDir("../app/src/main/kotlin")
        // Android platform seams: the harness supplies its own.
        exclude("**/ui/AndroidPlatform.kt")
        exclude("**/ui/AndroidGameScreen.kt")
        // The updater installs an APK over the running app; nothing else can do that.
        exclude("**/ui/UpdatePanel.kt")
        exclude("**/update/**")
        // Activity, view model and persistence are all Android-bound.
        exclude("**/MainActivity.kt")
        exclude("**/GameViewModel.kt")
        exclude("**/data/**")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":core"))
    implementation(libs.kotlinx.coroutines.swing)

    implementation("org.jetbrains.compose.desktop:desktop-jvm:$composeVersion")
    implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
    implementation("org.jetbrains.compose.foundation:foundation:$composeVersion")
    implementation("org.jetbrains.compose.material3:material3:$composeVersion")
    implementation("org.jetbrains.compose.ui:ui:$composeVersion")
}

application {
    mainClass.set(
        providers.gradleProperty("mainClass").orElse("com.staatseigentum.kollaps.desktop.MainKt"),
    )
}
