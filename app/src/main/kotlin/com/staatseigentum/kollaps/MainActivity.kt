package com.staatseigentum.kollaps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.staatseigentum.kollaps.ui.GameScreen
import com.staatseigentum.kollaps.ui.theme.KollapsTheme
import com.staatseigentum.kollaps.update.UpdateViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            KollapsTheme {
                val model: GameViewModel = viewModel()
                val updateModel: UpdateViewModel = viewModel()

                // The simulation only runs while the app is actually on screen; everything the
                // player misses is credited as offline production on the way back in.
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_START -> model.onForeground()
                            Lifecycle.Event.ON_STOP -> model.onBackground()
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
