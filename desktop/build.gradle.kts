plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
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
        // Notifications and the scheduler behind them exist only on a phone.
        exclude("**/notify/**")
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
 *
 * Applied to every `JavaExec` rather than to `run` by name: the run task comes from the Compose
 * plugin now — the plain `application` plugin used to provide it, and having both meant two tasks
 * called `run`, which Gradle refuses outright.
 */
tasks.withType<JavaExec>().configureEach {
    workingDir = rootProject.projectDir
}

/**
 * The entry point, named once and read by both plugins.
 *
 * The screenshot job in CI overrides it with `-PmainClass=...ScreenshotsKt` to render pictures
 * instead of opening a window, so the property has to be what both the `run` task and the
 * installer read — otherwise the two would start different programs.
 */
private val entryPoint = providers.gradleProperty("mainClass")
    .orElse("com.staatseigentum.kollaps.desktop.MainKt")

/**
 * The playable build.
 *
 * `packageDistributionForCurrentOS` produces an installer for whatever machine is running it —
 * an MSI on Windows, a DEB on Linux — with a Java runtime bundled in, so the person installing
 * it needs nothing beyond the file. `createDistributable` produces the same thing as a folder,
 * which is what goes into the portable archive for people who would rather not install anything.
 *
 * The version has to be a plain three-part number: jpackage rejects anything else, and a `v`
 * prefix is exactly the kind of thing that fails an hour into a release build.
 */
compose.desktop {
    application {
        mainClass = entryPoint.get()

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
            )
            packageName = "Kollaps"
            packageVersion = providers.gradleProperty("appVersion").orElse("1.0.0").get()
            description = "Ein Idle-Clicker vom Meteoriten bis zum Schwarzen Loch"
            vendor = "Staatseigentum"

            windows {
                menuGroup = "Kollaps"
                // A stable UUID, so an installer upgrades the previous version in place instead
                // of leaving two entries in the list of installed programs.
                upgradeUuid = "6E2B1C64-5B7E-4C0E-9F3D-2A9C6B1F8D41"
                dirChooser = true
                shortcut = true
            }
        }
    }
}
