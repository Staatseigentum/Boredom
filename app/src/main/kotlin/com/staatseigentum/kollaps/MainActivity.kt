package com.staatseigentum.kollaps

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.staatseigentum.kollaps.notify.ReturnReminder
import com.staatseigentum.kollaps.ui.AndroidPlatform
import com.staatseigentum.kollaps.ui.GameScreen
import com.staatseigentum.kollaps.update.UpdateViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AndroidPlatform {
                val model: GameViewModel = viewModel()
                val updateModel: UpdateViewModel = viewModel()

                val context = LocalContext.current

                // Asked once, on the first launch that can ask. Declining costs nothing but the
                // reminder — every other part of the game is untouched by the answer.
                val askForNotifications = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { }
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !ReturnReminder.isAllowed(context)
                    ) {
                        askForNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                // The simulation only runs while the app is actually on screen; everything the
                // player misses is credited as offline production on the way back in.
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_START -> {
                                model.onForeground()
                                // Whoever is looking at the game does not need reminding of it.
                                ReturnReminder.cancel(context)
                            }

                            Lifecycle.Event.ON_STOP -> {
                                model.onBackground()
                                val text = model.reminderText()
                                if (text == null) {
                                    ReturnReminder.cancel(context)
                                } else {
                                    ReturnReminder.schedule(
                                        context = context,
                                        capSeconds = model.offlineCapSeconds(),
                                        summary = text,
                                    )
                                }
                            }
                            // The install permission is granted in the system settings, so the
                            // answer only arrives when we come back to the foreground.
                            Lifecycle.Event.ON_RESUME -> updateModel.refreshInstallPermission()
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                GameScreen(model, updateModel)
            }
        }
    }
}
