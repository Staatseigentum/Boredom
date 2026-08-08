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
 * The version, forced into the only shape jpackage accepts: MAJOR.MINOR.BUILD.
 *
 * This is a safety net with a real scar behind it. The release build passed `-PappVersion=2.2.0`
 * on the command line, the runner's log showed exactly that string, and Gradle still saw `2` —
 * somewhere between PowerShell, cmd and the batch wrapper the rest was lost, and the release died
 * at configuration time with "Illegal version for 'Msi'". The version now arrives by environment
 * variable, which no shell splits, and whatever arrives is reshaped here rather than trusted.
 *
 * Missing parts are filled with zero and each part is clamped to what the installer formats allow,
 * so a mangled or unusual version costs the exact number it names and never the whole release.
 */
fun packageVersionOf(raw: String): String {
    val numbers = raw.trim().removePrefix("v")
        .split('.')
        .map { part -> part.takeWhile(Char::isDigit) }
        .filter { it.isNotEmpty() }
        .map { it.toLong() }

    // MSI refuses a leading zero outright, so one is worth more than an accurate but unbuildable
    // number: shipping 1.x beats shipping nothing.
    val major = (numbers.getOrNull(0) ?: 1L).coerceIn(1L, 255L)
    val minor = (numbers.getOrNull(1) ?: 0L).coerceIn(0L, 255L)
    val build = (numbers.getOrNull(2) ?: 0L).coerceIn(0L, 65_535L)
    return "$major.$minor.$build"
}

/**
 * Environment first, Gradle property second.
 *
 * The property is kept because it is what a person building this by hand would reach for; the
 * environment variable is what CI uses, because it is the one path that does not run through a
 * shell's idea of where an argument ends.
 */
val appVersion: String = packageVersionOf(
    providers.environmentVariable("KOLLAPS_APP_VERSION")
        .orElse(providers.gradleProperty("appVersion"))
        .orElse("1.0.0")
        .get(),
)

// At info level rather than lifecycle: it is noise on every ordinary build and the one thing
// worth knowing when a release build produces a file with a surprising number in its name.
logger.info("Paketversion: $appVersion")

/**
 * The playable build.
 *
 * `packageDistributionForCurrentOS` produces an installer for whatever machine is running it —
 * an MSI on Windows, a DEB on Linux — with a Java runtime bundled in, so the person installing
 * it needs nothing beyond the file. `createDistributable` produces the same thing as a folder,
 * which is what goes into the portable archive for people who would rather not install anything.
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
            packageVersion = appVersion
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
