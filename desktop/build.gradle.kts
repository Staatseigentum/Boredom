plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    application
}

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

    // `currentOs` rather than the plain desktop artifact: it is what pulls in the Skia native
    // library for the machine doing the building. Without it everything compiles and then dies
    // at the first bitmap with "Cannot find libskiko-linux-x64.so".
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
}

/**
 * Run from the repository root, not from this module's directory. The harness reads the app's
 * font files by relative path and writes its screenshots where the CI job looks for them; both
 * assume the root, and both fail quietly if the process starts a directory deeper.
 */
tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}

application {
    mainClass.set(
        providers.gradleProperty("mainClass").orElse("com.staatseigentum.kollaps.desktop.MainKt"),
    )
}
