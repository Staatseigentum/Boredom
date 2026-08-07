package com.staatseigentum.kollaps.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.staatseigentum.kollaps.MainActivity
import com.staatseigentum.kollaps.R
import java.util.concurrent.TimeUnit

/**
 * The nudge that says the collectors filled up while the phone was away.
 *
 * Scheduled when the app goes to the background and cancelled when it comes back, so it only ever
 * fires for somebody who actually left. The delay is the player's own offline cap: that is the
 * moment their machines stop earning, which makes it the one moment where being told is worth
 * something rather than merely being an interruption.
 *
 * The text is computed when the reminder is scheduled and carried along as input, so the worker
 * never has to open the save. A worker that reads a database is a worker that can fail on a
 * migration; a worker that formats a string it was handed cannot.
 */
object ReturnReminder {

    private const val WORK_NAME = "kollaps-rueckkehr"
    private const val CHANNEL_ID = "kollaps-ertrag"
    private const val NOTIFICATION_ID = 1

    private const val KEY_TEXT = "text"

    /** Never sooner than this, however small the cap is. */
    private const val MIN_DELAY_MINUTES = 30L

    /**
     * Books the reminder for [capSeconds] from now.
     *
     * [summary] is the line the player will read. Passing it in rather than a number keeps every
     * decision about wording and formatting on this side of the process boundary.
     */
    fun schedule(context: Context, capSeconds: Long, summary: String) {
        val minutes = (capSeconds / 60).coerceAtLeast(MIN_DELAY_MINUTES)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(minutes, TimeUnit.MINUTES)
            .setInputData(Data.Builder().putString(KEY_TEXT, summary).build())
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    /** Called on the way back in: whoever is looking at the game does not need reminding. */
    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
    }

    /** Whether the system will actually deliver anything. */
    fun isAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    internal fun notify(context: Context, text: String) {
        if (!isAllowed(context)) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_yield),
                // Low: it is a game telling you it has been idle. It should appear, not buzz.
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.channel_yield_description) },
        )

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}

/** Posts the reminder. Plain [Worker] because formatting a string needs no coroutine. */
internal class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : Worker(context, params) {

    override fun doWork(): Result {
        val text = inputData.getString("text") ?: return Result.success()
        ReturnReminder.notify(applicationContext, text)
        return Result.success()
    }
}
