package com.staatseigentum.kollaps.update

import com.staatseigentum.kollaps.core.i18n.Lang
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.staatseigentum.kollaps.core.update.AppVersion
import com.staatseigentum.kollaps.core.update.AvailableUpdate
import com.staatseigentum.kollaps.core.update.ReleaseFeed
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/** What the updater is currently doing, and what the player can do about it. */
sealed interface UpdateState {
    /** Nothing has been checked yet in this session. */
    data object Idle : UpdateState

    data object Checking : UpdateState

    data object UpToDate : UpdateState

    data class Available(val update: AvailableUpdate) : UpdateState

    data class Downloading(val update: AvailableUpdate, val progress: Float) : UpdateState

    /** Downloaded and waiting for the system installer. */
    data class Ready(val update: AvailableUpdate, val file: File) : UpdateState

    /** Downloaded, but the app may not install packages yet. */
    data class NeedsPermission(val update: AvailableUpdate, val file: File) : UpdateState

    data class Failed(val message: String) : UpdateState
}

/**
 * Drives the update check. Kept apart from the game view model so a failing network call can
 * never disturb the simulation.
 */
class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val service = UpdateService(application)
    private val preferences =
        application.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /** The version currently running, for display. */
    val installedVersion: AppVersion? = service.installedVersion()

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /** Set when an automatic check found something, so the game can put a dialog in front. */
    private val _prompt = MutableStateFlow(false)
    val prompt: StateFlow<Boolean> = _prompt.asStateFlow()

    /**
     * Whether the waiting update is one that has to be installed rather than merely offered.
     *
     * True for a new major or minor version, false for a patch. What makes the difference is what
     * the release *did*: a patch fixes something invisible, a feature release changes the save and
     * the screen — and a save written by one and read by the other is the kind of fault nobody can
     * reproduce because both people are running "Kollaps".
     *
     * Deliberately answered from the version numbers rather than from a flag in the release, so
     * there is nothing to forget to set. The number already carries the meaning; this reads it.
     */
    val mandatory: StateFlow<Boolean> = _state
        .map { current ->
            val update = when (current) {
                is UpdateState.Available -> current.update
                is UpdateState.Downloading -> current.update
                is UpdateState.Ready -> current.update
                is UpdateState.NeedsPermission -> current.update
                // A failure must never lock anybody out: if the update cannot be fetched or
                // installed, the game has to stay playable. Blocking on something that is not
                // working is how an update turns into a brick.
                else -> null
            } ?: return@map false
            val installed = installedVersion ?: return@map false
            update.version.isBigStepFrom(installed)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var work: Job? = null

    /** Checks at most once every [CHECK_INTERVAL_MILLIS], quietly. */
    fun checkOnLaunch() {
        if (_state.value != UpdateState.Idle) return
        val last = preferences.getLong(KEY_LAST_CHECK, 0L)
        val now = System.currentTimeMillis()
        if (last != 0L && now - last < CHECK_INTERVAL_MILLIS && now >= last) return
        check(announce = true)
    }

    /**
     * Looks for a newer release.
     *
     * @param announce whether finding one should raise the dialog rather than only update the card
     */
    fun check(announce: Boolean = false) {
        if (work?.isActive == true) return
        work = viewModelScope.launch {
            _state.value = UpdateState.Checking
            val result = service.fetchLatest()
            preferences.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()

            result
                .onSuccess { release ->
                    val update = ReleaseFeed.updateFrom(release, installedVersion)
                    if (update == null) {
                        _state.value = UpdateState.UpToDate
                    } else {
                        _state.value = UpdateState.Available(update)
                        if (announce) _prompt.value = true

                        /*
                         * A big step starts fetching itself.
                         *
                         * Android will not install anything without a tap — the package installer
                         * is the system's, and asking is the whole point of it — so "automatic"
                         * can only ever mean the half that is allowed: by the time the player has
                         * read the dialog, the download is already running or done, and the only
                         * thing left is the confirmation the system insists on.
                         *
                         * Only for a big step. Fetching sixty megabytes over mobile data for a
                         * patch nobody has to install would be spending somebody else's money on
                         * their behalf.
                         */
                        if (installedVersion?.let(update.version::isBigStepFrom) == true) {
                            download()
                        }
                    }
                }
                .onFailure { failure ->
                    _state.value = UpdateState.Failed(
                        failure.message ?: Lang.t("Update-Prüfung fehlgeschlagen"),
                    )
                }
        }
    }

    fun download() {
        val update = (_state.value as? UpdateState.Available)?.update ?: return
        if (work?.isActive == true) return
        work = viewModelScope.launch {
            _state.value = UpdateState.Downloading(update, 0f)
            service.download(update) { progress ->
                val current = _state.value
                if (current is UpdateState.Downloading) {
                    _state.value = current.copy(progress = progress)
                }
            }
                .onSuccess { file -> _state.value = readyState(update, file) }
                .onFailure { failure ->
                    _state.value = UpdateState.Failed(
                        failure.message ?: "Download fehlgeschlagen",
                    )
                }
        }
    }

    /** Hands the downloaded file to the system installer. */
    fun install() {
        when (val current = _state.value) {
            is UpdateState.Ready -> {
                // The permission can be revoked between download and tap.
                if (!service.canInstallPackages()) {
                    _state.value = UpdateState.NeedsPermission(current.update, current.file)
                    return
                }
                if (!service.install(current.file)) {
                    _state.value = UpdateState.Failed(Lang.t("Der Installer ließ sich nicht öffnen"))
                }
            }

            is UpdateState.NeedsPermission -> {
                if (service.canInstallPackages()) {
                    _state.value = UpdateState.Ready(current.update, current.file)
                    install()
                } else {
                    service.openInstallPermissionSettings()
                }
            }

            else -> Unit
        }
    }

    /** Called when returning from the system settings, to pick the install back up. */
    fun refreshInstallPermission() {
        val current = _state.value
        if (current is UpdateState.NeedsPermission && service.canInstallPackages()) {
            _state.value = UpdateState.Ready(current.update, current.file)
        }
    }

    fun dismissPrompt() {
        _prompt.value = false
    }

    /** Forgets a failure so the card offers a fresh attempt. */
    fun reset() {
        work?.cancel()
        work = null
        _state.value = UpdateState.Idle
    }

    fun discardDownload() {
        service.clearDownloads()
        reset()
    }

    private fun readyState(update: AvailableUpdate, file: File): UpdateState =
        if (service.canInstallPackages()) {
            UpdateState.Ready(update, file)
        } else {
            UpdateState.NeedsPermission(update, file)
        }

    private companion object {
        const val PREFERENCES_NAME = "kollaps_updates"
        const val KEY_LAST_CHECK = "last_check"
        const val CHECK_INTERVAL_MILLIS = 6 * 60 * 60 * 1000L
    }
}
