/*
 * The iPhone build, on its own.
 *
 * A settings file of its own rather than a fourth `include` in the main one, and the reason is not
 * tidiness. `org.jetbrains.kotlin.jvm` and `org.jetbrains.kotlin.multiplatform` are the same jar,
 * and this project deliberately resolves plugins per subproject rather than from a root classpath
 * — which the root build file explains at length, because putting Kotlin on the root classpath is
 * what stops Kotlin's Android support from seeing AGP. With `:core` on the JVM plugin and `:ios`
 * on the multiplatform one, the two arrive through different class loaders, and Kotlin/Native's
 * shared toolchain service then refuses itself with the memorable
 *
 *     Cannot set the value of task ':ios:linkDebugExecutableIosArm64' property
 *     'kotlinNativeBundleBuildService' of type KotlinNativeBundleBuildService
 *     using a provider of type KotlinNativeBundleBuildService
 *
 * Building `:ios` alone makes that impossible: it is the only project there is. It can afford to
 * be, because it depends on no other module — the rules and the interface are compiled from the
 * other modules' source directories, not from their artefacts, which was already true of the
 * desktop harness.
 *
 * And it costs the Android and Windows releases nothing: neither of them ever configures this
 * file, so a broken iOS build cannot take a phone build down with it.
 *
 *     ./gradlew -c settings-ios.gradle.kts :ios:linkReleaseExecutableIosArm64
 */
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

    plugins {
        id("org.jetbrains.kotlin.multiplatform") version "2.0.21"
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
        // Compose Multiplatform's iOS artefacts and the skiko build that goes with them are
        // published here rather than to Maven Central.
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "Kollaps"

include(":ios")
