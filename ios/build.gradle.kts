import java.awt.image.BufferedImage
import javax.imageio.ImageIO

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

/**
 * The iPhone build.
 *
 * Same trick the desktop harness uses, one step further: the rules and the whole interface are
 * compiled straight out of `:core` and `:app`, so there is one game and three ways to look at it.
 * What made that possible was taking the last JVM words out of those sources — `String.format`,
 * `synchronized` and `System.currentTimeMillis` — after which the difference between an Android
 * phone and this one is a handful of files in this module.
 *
 * There is no Xcode project and no Swift. Kotlin/Native can produce an iOS *executable*, and an
 * `.app` is a folder with an executable, a plist and some resources in it — so `paket.sh` builds
 * the bundle out of what Gradle already made. That is one fewer generated project file to keep
 * honest, and it means the whole iOS build is `linkReleaseExecutableIosArm64` plus a shell script
 * anybody can read.
 *
 * Nothing here is signed. That is the point: the file this produces is an unsigned `.ipa`, and
 * whoever installs it signs it with their own Apple ID on their own device. See the README.
 */
kotlin {
    // The device, and the simulator on an Apple-silicon Mac for anybody who wants to try it
    // without a phone in hand. Only the first is built in CI.
    iosArm64()
    iosSimulatorArm64()

    /*
     * Asked for by name rather than waited for.
     *
     * The shared `iosMain` between the device and the simulator comes from the default hierarchy,
     * which is applied late enough that reaching for the source set below finds nothing —
     * "KotlinSourceSet with name 'iosMain' not found", from a template that was going to create it
     * a moment later. Applying it here puts it in front of the code that needs it.
     */
    applyDefaultHierarchyTemplate()

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.executable {
            baseName = "Kollaps"
            // The entry point is not in the root package, so it has to be named.
            entryPoint = "com.staatseigentum.kollaps.ios.main"
            // Named rather than left to the platform klib. `UIApplicationMain` is reached through
            // the delegate class by name, not by a symbol the linker can see being used, and a
            // framework that nothing appears to reference is a framework that can be dropped.
            linkerOpts("-framework", "UIKit")
        }
    }

    sourceSets {
        val iosMain by getting {
            /*
             * The rules and the screen, compiled from where they already live.
             *
             * Copying them would be the obvious alternative and it is exactly what must not
             * happen: a second copy of the interface is a second copy that stops matching the
             * first the next time a panel changes. The exclusions are the same list the desktop
             * module keeps, written out one by one rather than by pattern, so that a new file has
             * to be considered rather than silently skipped.
             */
            kotlin.srcDir("../core/src/main/kotlin")
            kotlin.srcDir("../app/src/main/kotlin")
            // Android platform seams: this module supplies its own.
            kotlin.exclude("**/ui/AndroidPlatform.kt")
            kotlin.exclude("**/ui/AndroidGameScreen.kt")
            // The updater installs an APK over the running app. On a phone that signs its own
            // apps there is nothing it could do even if it could run.
            kotlin.exclude("**/ui/UpdatePanel.kt")
            kotlin.exclude("**/update/**")
            // Activity, view model and persistence are all Android-bound.
            kotlin.exclude("**/MainActivity.kt")
            kotlin.exclude("**/GameViewModel.kt")
            kotlin.exclude("**/data/**")
            // Notifications and the scheduler behind them are Android's.
            kotlin.exclude("**/notify/**")

            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(libs.kotlinx.serialization.json)
                // Named rather than taken from Compose transitively: the audio renders its cues off
                // the main thread, and a direct import deserves a direct dependency.
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

/**
 * The home-screen icon, from the same picture the phone and the PC already use.
 *
 * iOS wants one square at 1024 and it wants it opaque — an icon with an alpha channel is rejected
 * outright by the asset compiler, and the launcher PNG has one. So it is flattened onto the
 * game's own background rather than onto white, which is what it sits on everywhere else.
 *
 * Scaled by nearest neighbour, deliberately. Every other scaler averages neighbouring pixels, and
 * averaging is precisely what pixel art must not have done to it: a smooth 1024 icon of a
 * sixty-four pixel planet is a blurry circle.
 */
val appIcon by tasks.registering {
    val source = rootProject.layout.projectDirectory
        .file("app/src/main/res/mipmap-xxxhdpi/ic_launcher.png")
    val target = layout.buildDirectory.file("icon/AppIcon-1024.png")
    inputs.file(source)
    outputs.file(target)

    doLast {
        val small = ImageIO.read(source.asFile)
        val side = 1_024
        val large = BufferedImage(side, side, BufferedImage.TYPE_INT_RGB)
        val background = 0x05060F
        for (y in 0 until side) {
            val from = y * small.height / side
            for (x in 0 until side) {
                val pixel = small.getRGB(x * small.width / side, from)
                // Anything not fully opaque becomes the background outright. A pixel-art icon has
                // no soft edges to preserve, and blending would invent them.
                val opaque = if (pixel ushr 24 >= 0x80) pixel and 0xFFFFFF else background
                large.setRGB(x, y, opaque)
            }
        }
        target.get().asFile.apply {
            parentFile.mkdirs()
            ImageIO.write(large, "png", this)
        }
    }
}

/** The bundle script reads both, so the icon has to exist by the time the executable does. */
tasks.matching { it.name.startsWith("link") }.configureEach { dependsOn(appIcon) }
