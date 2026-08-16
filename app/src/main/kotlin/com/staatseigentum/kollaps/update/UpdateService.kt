package com.staatseigentum.kollaps.update

import com.staatseigentum.kollaps.core.i18n.Lang
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.staatseigentum.kollaps.core.update.AppVersion
import com.staatseigentum.kollaps.core.update.AvailableUpdate
import com.staatseigentum.kollaps.core.update.Release
import com.staatseigentum.kollaps.core.update.ReleaseFeed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

/**
 * Everything the updater needs from the outside world: the release feed, the download and the
 * system installer.
 *
 * The app is sideloaded rather than shipped through a store, so it has to look after its own
 * updates. It only ever reads the public release feed of its own repository and hands the
 * downloaded file to the system installer — it never installs anything by itself.
 */
class UpdateService(private val context: Context) {

    /** The version this app was built as, or `null` if the package manager will not say. */
    fun installedVersion(): AppVersion? = try {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        AppVersion.parse(info.versionName)
    } catch (e: Exception) {
        Log.w(TAG, Lang.t("Eigene Version nicht lesbar"), e)
        null
    }

    /** Fetches the newest published release, or fails with a message meant for the player. */
    suspend fun fetchLatest(allowPrereleases: Boolean = false): Result<Release?> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (allowPrereleases) {
                    ReleaseFeed.parseList(get(ALL_RELEASES_URL), allowPrereleases = true)
                } else {
                    ReleaseFeed.parse(get(LATEST_RELEASE_URL))
                }
            }.onFailure { if (it is CancellationException) throw it }
        }

    /**
     * Downloads the APK into the cache directory, reporting progress in `0f..1f`.
     *
     * Older downloads are cleared first so a cancelled or failed attempt cannot pile up.
     */
    suspend fun download(
        update: AvailableUpdate,
        onProgress: (Float) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val directory = File(context.cacheDir, DOWNLOAD_DIRECTORY).apply { mkdirs() }
            directory.listFiles()?.forEach { it.delete() }
            val target = File(directory, "kollaps-${update.version.canonical()}.apk")

            val connection = open(update.downloadUrl)
            try {
                val code = connection.responseCode
                if (code !in 200..299) throw IOException(Lang.t("Download fehlgeschlagen (HTTP %s)", code))

                val declared = connection.contentLengthLong
                val total = if (update.sizeBytes > 0) update.sizeBytes else declared

                connection.inputStream.use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(BUFFER_BYTES)
                        var written = 0L
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            written += read
                            if (total > 0) {
                                onProgress((written.toDouble() / total).toFloat().coerceIn(0f, 1f))
                            }
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }

            if (target.length() <= 0L) throw IOException(Lang.t("Die heruntergeladene Datei ist leer"))
            onProgress(1f)
            target
        }.onFailure { if (it is CancellationException) throw it }
    }

    /** True once the user has allowed this app to install packages. Always true below API 26. */
    fun canInstallPackages(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    /** Sends the user to the system screen where that permission is granted. */
    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure { Log.w(TAG, Lang.t("Einstellungsseite nicht erreichbar"), it) }
    }

    /** Hands the downloaded APK to the system installer. The user still confirms the install. */
    fun install(file: File): Boolean = runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}$PROVIDER_SUFFIX", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, APK_MIME_TYPE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
        true
    }.getOrElse {
        Log.w(TAG, Lang.t("Installation konnte nicht gestartet werden"), it)
        false
    }

    /** Throws away anything left in the download directory. */
    fun clearDownloads() {
        runCatching { File(context.cacheDir, DOWNLOAD_DIRECTORY).listFiles()?.forEach { it.delete() } }
    }

    // ------------------------------------------------------------------ http

    private fun get(url: String): String {
        val connection = open(url)
        try {
            when (val code = connection.responseCode) {
                in 200..299 -> Unit
                HttpURLConnection.HTTP_NOT_FOUND -> throw IOException(
                    Lang.t("Keine Veröffentlichungen gefunden. Ist das Repository öffentlich?"),
                )

                403 -> throw IOException(Lang.t("GitHub hat die Anfrage abgelehnt. Später nochmal versuchen."))
                else -> throw IOException(Lang.t("Server antwortete mit HTTP %s", code))
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Kollaps-Updater")
        }

    companion object {
        /**
         * Where the updater looks for new versions. Point this somewhere else to distribute the
         * game from another host — nothing outside this constant knows about GitHub.
         */
        const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/Staatseigentum/Boredom/releases/latest"

        const val ALL_RELEASES_URL =
            "https://api.github.com/repos/Staatseigentum/Boredom/releases?per_page=10"

        const val RELEASES_PAGE_URL = "https://github.com/Staatseigentum/Boredom/releases"

        private const val PROVIDER_SUFFIX = ".updates"
        private const val DOWNLOAD_DIRECTORY = "updates"
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        private const val BUFFER_BYTES = 16 * 1024
        private const val CONNECT_TIMEOUT_MILLIS = 15_000
        private const val READ_TIMEOUT_MILLIS = 30_000
        private const val TAG = "UpdateService"
    }
}
