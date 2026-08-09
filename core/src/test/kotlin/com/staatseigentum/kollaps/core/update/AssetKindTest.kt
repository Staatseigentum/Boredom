package com.staatseigentum.kollaps.core.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Which file each version of the game is allowed to install.
 *
 * A release now carries three: an APK, a Windows installer and a portable archive. The phone can
 * only use the first and the PC only the second, and handing either one the other's file would be
 * a download that ends in an error the player cannot do anything about.
 */
class AssetKindTest {

    private val release = Release(
        tag = "v9.9.9",
        assets = listOf(
            ReleaseAsset("kollaps-9.9.9.apk", "https://example.invalid/a.apk", 1_000),
            ReleaseAsset("Kollaps-9.9.9-setup.msi", "https://example.invalid/a.msi", 60_000),
            ReleaseAsset("Kollaps-9.9.9-windows-portabel.zip", "https://example.invalid/a.zip", 60_000),
        ),
    )

    private val old = AppVersion.parse("1.0.0")

    @Test
    fun `the phone is offered the apk`() {
        val update = ReleaseFeed.updateFrom(release, old, kind = AssetKind.APK)
        assertNotNull(update)
        assertEquals("https://example.invalid/a.apk", update.downloadUrl)
    }

    @Test
    fun `the pc is offered the installer, not the archive next to it`() {
        val update = ReleaseFeed.updateFrom(release, old, kind = AssetKind.WINDOWS_INSTALLER)
        assertNotNull(update)
        assertEquals("https://example.invalid/a.msi", update.downloadUrl)
        assertEquals(60_000, update.sizeBytes)
    }

    @Test
    fun `asking for a file the release does not carry is no update at all`() {
        val phoneOnly = release.copy(assets = release.assets.filter { it.isApk })
        assertNull(ReleaseFeed.updateFrom(phoneOnly, old, kind = AssetKind.WINDOWS_INSTALLER))
        assertNotNull(ReleaseFeed.updateFrom(phoneOnly, old, kind = AssetKind.APK))
    }

    @Test
    fun `the default stays the apk, so the phone needs no change`() {
        assertEquals(
            ReleaseFeed.updateFrom(release, old, kind = AssetKind.APK)?.downloadUrl,
            ReleaseFeed.updateFrom(release, old)?.downloadUrl,
        )
    }

    @Test
    fun `an installer for a version already installed is not offered`() {
        val current = AppVersion.parse("9.9.9")
        assertNull(ReleaseFeed.updateFrom(release, current, kind = AssetKind.WINDOWS_INSTALLER))
    }
}
