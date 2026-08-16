package com.staatseigentum.kollaps.ios

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.staatseigentum.kollaps.core.GameState
import com.staatseigentum.kollaps.core.NumberFormat
import com.staatseigentum.kollaps.core.Numbers
import com.staatseigentum.kollaps.core.Tiers
import com.staatseigentum.kollaps.core.Wallclock
import com.staatseigentum.kollaps.core.i18n.Lang
import com.staatseigentum.kollaps.core.i18n.Language
import com.staatseigentum.kollaps.ui.GameScreen
import com.staatseigentum.kollaps.ui.PlainGame
import com.staatseigentum.kollaps.ui.SaveSlotPanel
import com.staatseigentum.kollaps.ui.SlotSummary
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectBase
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toCValues
import platform.Foundation.NSDate
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.NSStringFromClass
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDelegateProtocol
import platform.UIKit.UIApplicationDelegateProtocolMeta
import platform.UIKit.UIApplicationMain
import platform.UIKit.UIResponder
import platform.UIKit.UIResponderMeta
import platform.UIKit.UIScreen
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow

/**
 * The whole iPhone application: an entry point, a delegate and one screen.
 *
 * There is no Xcode project behind this. An iOS app is a Mach-O executable, a plist and a folder
 * of resources, and Kotlin/Native can produce the executable — so it does, and `paket.sh` puts the
 * folder together. That removes a generated project file nobody could review from the middle of
 * the release path, and it is why this file starts where a Swift `@main` normally would.
 */

/** Handed to `UIApplicationMain`, which builds the delegate below and never returns. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun main() {
    memScoped {
        val arguments = arrayOf("Kollaps")
        autoreleasepool {
            UIApplicationMain(
                arguments.size,
                arguments.map { it.cstr.ptr }.toCValues().ptr,
                null,
                NSStringFromClass(KollapsDelegate),
            )
        }
    }
}

/**
 * One window, one view controller, and the two moments a save has to be written.
 *
 * The second is the part that is genuinely different from the other platforms. A desktop window
 * is closed and a phone app is *suspended* — it keeps existing, stops getting frames, and may be
 * killed later without being told. So the save goes out when the app leaves the screen, not when
 * it ends, because being ended is not something it will be present for.
 */
@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
class KollapsDelegate : UIResponder, UIApplicationDelegateProtocol {

    companion object : UIResponderMeta(), UIApplicationDelegateProtocolMeta

    @ObjCObjectBase.OverrideInit
    constructor() : super()

    private var held: UIWindow? = null

    override fun window(): UIWindow? = held

    override fun setWindow(window: UIWindow?) {
        held = window
    }

    override fun application(
        application: UIApplication,
        didFinishLaunchingWithOptions: Map<Any?, *>?,
    ): Boolean {
        // Before anything reads the time: the save about to be loaded is credited for the hours
        // the app was away, and that is the first thing that happens.
        Wallclock.readFrom { (NSDate().timeIntervalSince1970 * 1_000.0).toLong() }

        // What the phone says it speaks. The save overrides it the moment one is loaded with a
        // language in it; see GameEngine.applyLanguage, which the game driver calls.
        Lang.current = Language.ofLocale(NSLocale.currentLocale.languageCode)

        val window = UIWindow(frame = UIScreen.mainScreen.bounds)
        window.rootViewController = mainViewController()
        window.makeKeyAndVisible()
        held = window
        return true
    }

    override fun applicationDidEnterBackground(application: UIApplication) {
        IosSave.save(Kollaps.game.state)
        // A cue caught halfway through would otherwise be resumed on the way back in, seconds
        // later and with nothing on screen to explain it.
        IosAudio.silence()
    }

    override fun applicationWillTerminate(application: UIApplication) {
        IosSave.save(Kollaps.game.state)
    }
}

/**
 * The running game, made once and kept.
 *
 * Outside the composition on purpose: the delegate has to be able to write the save when the app
 * is put away, and by then there is no composition to ask.
 */
object Kollaps {
    val game: PlainGame by lazy {
        val loaded = IosSave.load()
        // The number format is a display setting kept in the save, and it is ambient rather than
        // threaded through forty call sites — so it has to be put back by hand on the way in.
        Numbers.format = NumberFormat.byName(loaded?.numberFormat)
        PlainGame(loaded ?: GameState.new(Wallclock.millis())).also { fresh ->
            // Credited before the first frame, so the report is on screen when the app opens
            // rather than a second later.
            if (loaded != null) fresh.creditTimeAway()
        }
    }
}

/** The one screen, wrapped in what only this platform can provide. */
fun mainViewController(): UIViewController = ComposeUIViewController {
    IosPlatform {
        RunningGame(Kollaps.game)
    }
}

/**
 * Drives the simulation off the frame clock, exactly as the PC window does.
 *
 * Three clocks meet here: the frame clock advances production, the wall clock settles the lab and
 * the two automation rules that read it, and a slower loop writes the save. The frame clock stops
 * on its own when the app leaves the screen, which is the whole reason it is the right thing to
 * hang this on — a suspended app must not go on producing, because the offline credit is about to
 * pay for exactly those seconds and would pay for them twice.
 */
@Composable
private fun RunningGame(game: PlainGame, modifier: Modifier = Modifier) {
    LaunchedEffect(game) {
        var previous = 0L
        var sinceSave = 0.0
        var sinceTick = 0.0
        while (true) {
            withFrameNanos { now ->
                // The clock keeps running while the game is paused for the collapse sequence, and
                // only the tick is skipped — so those seconds are dropped rather than banked up
                // and paid out in one lump the moment the animation ends.
                if (previous != 0L && !game.paused) {
                    sinceTick += (now - previous) / 1_000_000_000.0
                    if (sinceTick >= TICK_SECONDS) {
                        game.tick(sinceTick)
                        game.settleWallClock()
                        sinceSave += sinceTick
                        sinceTick = 0.0
                        if (sinceSave >= AUTOSAVE_SECONDS) {
                            sinceSave = 0.0
                            IosSave.save(game.state)
                        }
                    }
                }
                previous = now
            }
        }
    }

    GameScreen(
        state = game.state,
        stats = game.stats,
        buyAmount = game.buyAmount,
        offlineReport = game.offlineReport,
        actions = game,
        modifier = modifier.fillMaxSize(),
        saveSlots = { Slots(game) },
    )
}

/**
 * The three save slots.
 *
 * Same panel the phone and the PC draw, filled from the user defaults. Switching writes the
 * running game out first, exactly as on the others — the order is the safety of the whole feature.
 */
@Composable
private fun Slots(game: PlainGame) {
    var active by remember { mutableStateOf(IosSave.activeSlot()) }
    var summaries by remember { mutableStateOf(emptyList<SlotSummary>()) }

    LaunchedEffect(active, game.state.collapses, game.state.bigBangs) {
        summaries = (0 until IosSave.SLOTS).map { index ->
            val state = if (index == active) game.state else IosSave.load(index)
            SlotSummary(
                index = index,
                detail = describeSlot(state),
                isActive = index == active,
                isEmpty = state == null,
            )
        }
    }

    if (summaries.isEmpty()) return
    SaveSlotPanel(
        slots = summaries,
        onSwitch = { target ->
            IosSave.save(game.state, active)
            IosSave.setActiveSlot(target)
            active = target
            game.load(IosSave.load(target))
        },
    )
}

/** One line saying what is in a slot, or that there is nothing in it. */
private fun describeSlot(state: GameState?): String {
    if (state == null) return "Leer — hier fängt ein neues Spiel an."
    val parts = buildList {
        add(Tiers.forMass(state.runMass).name)
        if (state.collapses > 0) add("${state.collapses} Kollapse")
        if (state.bigBangs > 0) add("${state.bigBangs} Urknalle")
    }
    return parts.joinToString(" · ")
}

/** How often the rules are advanced, in seconds. The same rate the phone and the PC use. */
private const val TICK_SECONDS = 0.1

private const val AUTOSAVE_SECONDS = 12.0
