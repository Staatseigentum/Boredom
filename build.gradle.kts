/**
 * Deliberately empty, and it has to stay that way.
 *
 * Declaring the Kotlin plugin here puts it on the root classpath while the Android plugin stays
 * in `:app`, and Kotlin's Android support then cannot see AGP at all:
 *
 *     Could not generate a decorated class for type KotlinAndroidTarget.
 *        > com/android/build/gradle/api/BaseVariant
 *
 * Declaring the Android plugin here as well would fix that and break the other end: naming it
 * makes Gradle resolve it before any project is configured, so the desktop harness could not be
 * built without the Android toolchain. Each module brings its own plugins, and the versions are
 * pinned in `settings.gradle.kts`, which is what keeps them from disagreeing.
 */
