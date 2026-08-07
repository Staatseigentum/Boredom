/**
 * Deliberately empty.
 *
 * Declaring the plugins here with `apply false` only pinned versions, which the version catalog
 * already does — but it also made Gradle resolve every plugin, the Android one included, before
 * any project was configured. That meant the desktop harness could not be built without the
 * Android toolchain, which defeats the point of having a harness that runs anywhere.
 *
 * Each module now brings its own plugins, so `gradle --configure-on-demand :desktop:run` touches
 * nothing Android at all.
 */
