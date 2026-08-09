package com.staatseigentum.kollaps.desktop

import com.staatseigentum.kollaps.core.update.AppVersion
import com.staatseigentum.kollaps.core.update.AssetKind
import com.staatseigentum.kollaps.core.update.AvailableUpdate
import com.staatseigentum.kollaps.core.update.ReleaseFeed
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

/**
 * The PC version keeping itself up to date.
 *
 * Same reason the phone has one: this is not shipped through a store, so nothing else is going to
 * tell anybody a new version exists. It reads the public release feed of its own repository,
 * downloads the installer for the version it finds, and hands that file to the system installer —
 * it never patches itself in place.
 *
 * On Windows the installer carries a stable upgrade code, which is what makes `msiexec` replace
 * the installed program rather than putting a second copy beside it. That property is the whole
 * reason this can be one click instead of a download link.
 */
object DesktopUpdater {

    /** Where the release feed lives. The same one the phone reads. */
    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/Staatseigentum/Boredom/releases/latest"

    const val RELEASES_PAGE_URL = "https://github.com/Staatseigentum/Boredom/releases"

    /**
     * The version this build was packaged as.
     *
     * Handed in by the build through a system property rather than read from a file: the
     * installer already knows the number, and a second place to write it down is a second place
     * for it to be wrong. Absent when running from a checkout, which reads as "do not offer an
     * update to a developer build".
     */
    fun installedVersion(): AppVersion? =
        AppVersion.parse(System.getProperty("kollaps.version"))

    /** True only where an installer can actually be run. */
    val canInstall: Boolean
        get() = System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)

    /**
     * Asks the feed whether there is something newer, or `null` if there is not.
     *
     * Every failure — no network, a rate limit, a feed that changed shape — comes out as `null`.
     * An update check is the least important thing this program does, and it has no business
     * showing an error over a game.
     */
    fun check(): AvailableUpdate? = runCatching {
        val current = installedVersion() ?: return null
        ReleaseFeed.updateFrom(
            release = ReleaseFeed.parse(get(LATEST_RELEASE_URL)),
            current = current,
            kind = AssetKind.WINDOWS_INSTALLER,
        )
    }.getOrNull()

    /**
     * Downloads the installer, reporting progress in `0f..1f`, and returns the file.
     *
     * Written beside the temporary directory under a name that carries the version, and any older
     * download is cleared first so a cancelled attempt cannot pile up across a few releases.
     */
    fun download(update: AvailableUpdate, onProgress: (Float) -> Unit): File? = runCatching {
        val directory = File(System.getProperty("java.io.tmpdir"), "kollaps-update")
        directory.mkdirs()
        directory.listFiles()?.forEach { it.delete() }

        val target = File(directory, "Kollaps-${update.version.canonical()}-setup.msi")
        val connection = open(update.downloadUrl)
        connection.inputStream.use { source ->
            target.outputStream().use { sink ->
                val total = update.sizeBytes.takeIf { it > 0 } ?: connection.contentLengthLong
                val buffer = ByteArray(1 shl 16)
                var written = 0L
                while (true) {
                    val read = source.read(buffer)
                    if (read < 0) break
                    sink.write(buffer, 0, read)
                    written += read
                    if (total > 0) onProgress((written.toDouble() / total).toFloat().coerceIn(0f, 1f))
                }
            }
        }
        connection.disconnect()
        target
    }.getOrNull()

    /**
     * Hands the downloaded installer to Windows and asks this program to close.
     *
     * `msiexec` cannot replace files that are open, and the one file guaranteed to be open is the
     * one running this code. Quitting immediately after handing over is not politeness — it is the
     * only order in which the upgrade can succeed.
     */
    fun install(installer: File): Boolean = runCatching {
        if (!canInstall) return false
        ProcessBuilder("msiexec", "/i", installer.absolutePath).start()
        true
    }.getOrDefault(false)

    /** Opens the releases page in whatever the machine uses for the web. */
    fun openReleasesPage(): Boolean = runCatching {
        val desktop = java.awt.Desktop.getDesktop()
        desktop.browse(URI(RELEASES_PAGE_URL))
        true
    }.getOrDefault(false)

    private fun get(url: String): String? {
        val connection = open(url)
        return try {
            if (connection.responseCode !in 200..299) null else connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URI(url).toURL().openConnection() as HttpURLConnection).apply {
            // GitHub answers a request without a user agent with a 403, and the accept header is
            // what pins the response to the shape the parser was written against.
            setRequestProperty("User-Agent", "Kollaps")
            setRequestProperty("Accept", "application/vnd.github+json")
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            instanceFollowRedirects = true
        }

    private const val TIMEOUT_MILLIS = 15_000
}
