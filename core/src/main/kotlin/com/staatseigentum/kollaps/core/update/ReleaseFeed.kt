package com.staatseigentum.kollaps.core.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One file attached to a release. */
@Serializable
data class ReleaseAsset(
    val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = "",
    val size: Long = 0,
) {
    val isApk: Boolean get() = name.endsWith(".apk", ignoreCase = true)

    val isWindowsInstaller: Boolean get() = name.endsWith(".msi", ignoreCase = true)
}

/**
 * Which file a platform is looking for in a release.
 *
 * The feed carries three now — an APK, an installer and a portable archive — and each version of
 * the game can install exactly one of them. Asking for the wrong kind has to come out as "nothing
 * to update", never as a download the machine cannot use.
 */
enum class AssetKind(val matches: (ReleaseAsset) -> Boolean) {
    APK({ it.isApk }),
    WINDOWS_INSTALLER({ it.isWindowsInstaller }),
}

/** A release as GitHub reports it. Only the fields the updater actually needs. */
@Serializable
data class Release(
    @SerialName("tag_name") val tag: String = "",
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<ReleaseAsset> = emptyList(),
)

/** A release that is newer than what is installed and actually carries an APK. */
data class AvailableUpdate(
    val version: AppVersion,
    val title: String,
    val notes: String,
    val downloadUrl: String,
    val sizeBytes: Long,
)

/**
 * Turns the release feed into a yes-or-no answer: is there something newer to install?
 *
 * Kept free of any networking so the decision can be unit tested against real payloads.
 */
object ReleaseFeed {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /** Parses a single release object. Returns `null` for anything unreadable. */
    fun parse(raw: String?): Release? {
        if (raw.isNullOrBlank()) return null
        return try {
            json.decodeFromString(Release.serializer(), raw)
        } catch (_: Exception) {
            null
        }
    }

    /** Parses a list of releases and picks the newest usable one. */
    fun parseList(raw: String?, allowPrereleases: Boolean = false): Release? {
        if (raw.isNullOrBlank()) return null
        val releases = try {
            json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(Release.serializer()), raw)
        } catch (_: Exception) {
            return null
        }
        return releases
            .filter { it.isUsable(allowPrereleases) }
            .maxByOrNull { AppVersion.parse(it.tag) ?: AppVersion(listOf(0), it.tag) }
    }

    /**
     * The update [release] represents, or `null` when there is nothing to offer — because the
     * release is a draft, is older than [current], or has no APK attached.
     */
    fun updateFrom(
        release: Release?,
        current: AppVersion?,
        allowPrereleases: Boolean = false,
        kind: AssetKind = AssetKind.APK,
    ): AvailableUpdate? {
        if (release == null || !release.isUsable(allowPrereleases)) return null

        val version = AppVersion.parse(release.tag) ?: return null
        if (current != null && version <= current) return null

        val asset = release.assets.firstOrNull(kind.matches) ?: return null
        if (asset.downloadUrl.isBlank()) return null

        return AvailableUpdate(
            version = version,
            title = release.name?.takeIf { it.isNotBlank() } ?: "Version ${version.canonical()}",
            notes = release.body?.trim().orEmpty(),
            downloadUrl = asset.downloadUrl,
            sizeBytes = asset.size,
        )
    }

    private fun Release.isUsable(allowPrereleases: Boolean): Boolean =
        !draft && (allowPrereleases || !prerelease) && tag.isNotBlank()
}
