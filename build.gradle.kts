/**
 * Loads the Kotlin plugin once for the whole build.
 *
 * Nothing is applied here; the point is the shared classpath. Without it each module loads the
 * Kotlin Gradle plugin separately, which Gradle warns is unsupported and "may break the build".
 * No version is given — those are pinned in `settings.gradle.kts`, which is what lets the
 * Android plugin stay out of this block so the desktop harness can be built without it.
 */
plugins {
    id("org.jetbrains.kotlin.jvm") apply false
}
