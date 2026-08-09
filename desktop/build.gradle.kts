import java.io.ByteArrayOutputStream

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

/**
 * The pixel fonts travel with the build.
 *
 * They live in the app module because that is where Android needs them, and copying rather than
 * duplicating keeps one copy in the repository. Without this the packaged desktop build had no
 * fonts at all and quietly rendered the whole game in the system monospace — it looked like a
 * different program, and nothing failed to say so.
 */
tasks.named<ProcessResources>("processResources") {
    from("../app/src/main/res/font") {
        include("*.ttf")
        into("font")
    }
    // The same icon the phone puts on its home screen, for the window and the taskbar.
    from("../app/src/main/res/mipmap-xxxhdpi") {
        include("ic_launcher.png")
        rename { "icon.png" }
    }
}

/**
 * The launcher icon, wrapped in an ICO container for the Windows installer.
 *
 * jpackage wants an `.ico` on Windows and the repository has a PNG — so rather than committing a
 * second copy of the same picture in a second format, the container is written here. An ICO may
 * hold a PNG verbatim (Windows has understood that since Vista), which makes the whole file a
 * twenty-two byte header in front of the bytes that already exist. One icon, one source.
 */
val windowsIcon by tasks.registering {
    val source = layout.projectDirectory.file("../app/src/main/res/mipmap-xxxhdpi/ic_launcher.png")
    val target = layout.buildDirectory.file("icon/kollaps.ico")
    inputs.file(source)
    outputs.file(target)

    doLast {
        val png = source.asFile.readBytes()
        val out = ByteArrayOutputStream()

        fun short(value: Int) {
            out.write(value and 0xFF)
            out.write(value shr 8 and 0xFF)
        }
        fun int(value: Int) {
            short(value and 0xFFFF)
            short(value shr 16 and 0xFFFF)
        }

        // ICONDIR: reserved, type 1 = icon, one image.
        short(0); short(1); short(1)
        // ICONDIRENTRY. A side of 256 is written as zero; 192 fits in the byte as itself.
        out.write(192); out.write(192)
        out.write(0); out.write(0)
        short(1); short(32)
        int(png.size)
        int(22)
        out.write(png)

        target.get().asFile.apply {
            parentFile.mkdirs()
            writeBytes(out.toByteArray())
        }
    }
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

        // How the running program learns which version it is. The installer already knows the
        // number; writing it into a second place would only give it somewhere to be wrong. Absent
        // when running from a checkout, which the updater reads as "do not offer an update".
        jvmArgs += "-Dkollaps.version=$appVersion"

        /*
         * A ceiling on the heap, because the default is a share of the machine.
         *
         * Without this the JVM sizes its maximum heap at a quarter of physical memory — a
         * gigabyte on a small laptop, four on a desktop — and then has no reason to collect
         * anything until it gets there. The game allocates steadily (a fresh immutable state
         * several times a second) and none of it is kept, so what a task manager showed was not a
         * leak but garbage nobody had asked the collector to deal with yet.
         *
         * Two hundred and fifty-six megabytes is far more than this needs: the largest thing in
         * memory is two sprite sheets and a sixteen-second music loop. Capping it means the
         * collector runs when it should, and the number in the task manager is the number the
         * game actually uses.
         */
        jvmArgs += "-Xmx256m"

        // The heap starts where it will settle, so the first minute is not a series of growth
        // pauses on a machine that was always going to give it this much.
        jvmArgs += "-Xms64m"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
            )
            packageName = "Kollaps"
            packageVersion = appVersion
            description = "Ein Idle-Clicker vom Meteoriten bis zum Schwarzen Loch"
            vendor = "Staatseigentum"

            // Both formats come from the one PNG: the ICO is generated, the PNG is used as it is.
            windows {
                iconFile.set(layout.buildDirectory.file("icon/kollaps.ico").get().asFile)
                menuGroup = "Kollaps"
                // A stable UUID, so an installer upgrades the previous version in place instead
                // of leaving two entries in the list of installed programs.
                upgradeUuid = "6E2B1C64-5B7E-4C0E-9F3D-2A9C6B1F8D41"
                dirChooser = true
                shortcut = true
            }

            linux {
                iconFile.set(
                    rootProject.file("app/src/main/res/mipmap-xxxhdpi/ic_launcher.png"),
                )
            }
        }
    }
}

// The packaging tasks read the icon file, so it has to exist before they run. Named rather than
// inferred: `iconFile` takes a plain File, which carries no provenance for Gradle to follow.
tasks.matching { it.name.startsWith("package") || it.name == "createDistributable" }
    .configureEach { dependsOn(windowsIcon) }
