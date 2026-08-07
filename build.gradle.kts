/**
 * Version pinning only — nothing here is applied.
 *
 * The Kotlin plugins have to be declared at the root even though no root project uses them:
 * loading the Kotlin Gradle plugin separately per subproject is unsupported and warns that it
 * "may break the build". Declaring it once here puts it on a shared classpath.
 *
 * The Android plugin is deliberately absent. Naming it here — even with `apply false` — makes
 * Gradle resolve it before any project is configured, which meant the desktop harness could not
 * be built without the Android toolchain. It is declared in `:app`, the only module that needs
 * it, so `gradle --configure-on-demand :desktop:run` touches nothing Android at all.
 */
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
}
