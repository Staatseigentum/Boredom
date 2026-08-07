pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }

    /**
     * Plugin versions live here rather than in a root `plugins { ... apply false }` block.
     *
     * That block resolved every plugin before any project was configured, so building the
     * desktop harness needed the Android toolchain. Declaring versions per subproject instead
     * fails the other way: Kotlin's JVM and Android plugins are the same jar, so whichever loads
     * second is refused with "already on the classpath with an unknown version". Pinning here
     * fixes both — each plugin resolves only when a module asks for it, and modules ask without
     * a version, so there is never a second opinion about which one to use.
     */
    plugins {
        id("com.android.application") version "8.7.3"
        id("org.jetbrains.kotlin.android") version "2.0.21"
        id("org.jetbrains.kotlin.jvm") version "2.0.21"
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
        id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
        id("org.jetbrains.compose") version "1.7.3"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Kollaps"

include(":app")
include(":core")
include(":desktop")
