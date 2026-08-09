package com.staatseigentum.kollaps.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.staatseigentum.kollaps.MainActivity
import com.staatseigentum.kollaps.R

/**
 * The line that stays in the shade while the game is closed.
 *
 * Deliberately not a foreground service. A service would let the numbers tick, and it would also
 * mean an idle game holding a process open and asking for a permission that exists for navigation
 * and music — far more than this is worth. What is posted instead is a plain ongoing notification,
 * and the two things on it are chosen so that they stay true without one:
 *
 * - production per second does not change while the app is closed, so a number taken on the way
 *   out is still the right number an hour later;
 * - the lab countdown is handed to the system as a target time and ticks by itself, because a
 *   chronometer is drawn by the notification shade and not by us.
 *
 * Anything that would need updating — mass in hand, the tier — is left off rather than shown going
 * stale, which is the only way this can be honest about being a snapshot.
 */
object LiveStatus {

    private const val CHANNEL_ID = "kollaps-status"
    private const val NOTIFICATION_ID = 2

    /**
     * Posts or replaces the status line.
     *
     * [researchDoneAtMillis] turns the second line into a live countdown; `null` leaves whatever
     * [detail] says standing on its own.
     */
    fun show(
        context: Context,
        headline: String,
        detail: String?,
        researchDoneAtMillis: Long? = null,
    ) {
        if (!ReturnReminder.isAllowed(context)) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_status),
                // The lowest there is: this is a line to glance at, never a thing that arrives.
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                description = context.getString(R.string.channel_status_description)
                setShowBadge(false)
            },
        )

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(headline)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            // Ongoing and un-cancellable by swipe: it is a status, and one that comes back the
            // moment the app is closed again would be worse than one that simply stays.
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setContentIntent(open)

        detail?.let { builder.setContentText(it) }

        if (researchDoneAtMillis != null) {
            // The shade counts this down on its own. `setWhen` is the target, not the start.
            builder.setWhen(researchDoneAtMillis)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
        }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    /** Takes it away. Called the moment somebody is looking at the game itself. */
    fun hide(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
