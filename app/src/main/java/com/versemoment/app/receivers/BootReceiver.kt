package com.versemoment.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.versemoment.app.MainActivity
import com.versemoment.app.data.PreferencesRepository
import com.versemoment.app.data.VerseMomentDatabase
import com.versemoment.app.data.VerseRepository
import com.versemoment.app.domain.SelectionMode
import com.versemoment.app.domain.WallpaperApplyStatus
import com.versemoment.app.domain.WallpaperUpdateResult
import com.versemoment.app.scheduling.AutomationScheduler
import com.versemoment.app.wallpaper.WallpaperRenderer
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            -> scheduleIfEnabled(context)

            AutomationScheduler.ACTION_UPDATE_WALLPAPER -> updateWallpaper(context)
        }
    }

    private fun scheduleIfEnabled(context: Context) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val preferences = PreferencesRepository(context.applicationContext)
                val scheduler = AutomationScheduler(context.applicationContext)
                if (preferences.automationEnabledFlow.first()) {
                    scheduler.scheduleNextUpdate(preferences.updateIntervalFlow.first())
                } else {
                    scheduler.cancel()
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Could not restore wallpaper automation schedule", exception)
            }
        }
    }

    private fun updateWallpaper(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val appContext = context.applicationContext
            val preferences = PreferencesRepository(appContext)
            val scheduler = AutomationScheduler(appContext)
            var enabled = false
            var intervalMinutes = 5
            try {
                enabled = preferences.automationEnabledFlow.first()
                intervalMinutes = preferences.updateIntervalFlow.first()
                if (!enabled) return@launch
                preferences.recordAutomationAttempt(System.currentTimeMillis())

                val database = VerseMomentDatabase.getInstance(appContext)
                val repository = VerseRepository(database.verseDao(), appContext.assets)
                repository.seedTranslationsAndVerses()

                val translationId = preferences.selectedTranslationFlow.first()
                val verses = repository.allVerses(translationId)
                val mostRecentSuccess = repository.history(20)
                    .firstOrNull { it.status == WallpaperApplyStatus.SUCCESS }
                    ?.verseId
                val candidates = verses.filterNot { it.id == mostRecentSuccess }.ifEmpty { verses }
                val verse = candidates.randomOrNull()
                    ?: throw IllegalStateException("No verses are available for the selected translation.")
                val renderer = WallpaperRenderer(appContext)
                val selectedAt = System.currentTimeMillis()

                try {
                    val background = renderer.loadOriginalBackground()
                    renderer.applyWallpaper(renderer.renderWallpaper(verse, background))
                    repository.recordHistory(
                        WallpaperUpdateResult(
                            id = UUID.randomUUID().toString(),
                            verseId = verse.id,
                            selectionMode = SelectionMode.DIVINE_DISCOVERY,
                            selectedAt = selectedAt,
                            appliedAt = System.currentTimeMillis(),
                            status = WallpaperApplyStatus.SUCCESS,
                            errorCode = null,
                            errorMessage = null,
                        ),
                    )
                    preferences.recordAutomationSuccess(System.currentTimeMillis())
                    NotificationManagerCompat.from(appContext).cancel(UPDATE_NOTIFICATION_ID)
                } catch (exception: Exception) {
                    repository.recordHistory(
                        WallpaperUpdateResult(
                            id = UUID.randomUUID().toString(),
                            verseId = verse.id,
                            selectionMode = SelectionMode.DIVINE_DISCOVERY,
                            selectedAt = selectedAt,
                            appliedAt = null,
                            status = WallpaperApplyStatus.FAILED,
                            errorCode = "WALLPAPER_APPLY_FAILED",
                            errorMessage = exception.message ?: exception.javaClass.simpleName,
                        ),
                    )
                    throw exception
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Scheduled wallpaper update failed", exception)
                try {
                    val message = exception.message ?: exception.javaClass.simpleName
                    preferences.recordAutomationFailure(System.currentTimeMillis(), message)
                    postFailureNotification(appContext, message)
                } catch (statusException: Exception) {
                    Log.e(TAG, "Could not save or notify about the wallpaper update failure", statusException)
                }
            } finally {
                try {
                    enabled = preferences.automationEnabledFlow.first()
                    intervalMinutes = preferences.updateIntervalFlow.first()
                    if (enabled) scheduler.scheduleNextUpdate(intervalMinutes) else scheduler.cancel()
                } catch (exception: Exception) {
                    Log.e(TAG, "Could not schedule the next wallpaper update", exception)
                }
                pendingResult.finish()
            }
        }
    }

    private fun postFailureNotification(context: Context, error: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
            ?: throw IllegalStateException("Notification service is unavailable.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Wallpaper automation",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
        val openAppIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val message = if (error.contains("current lock-screen wallpaper", ignoreCase = true)) {
            "Open VerseMoment and choose a background image in Wallpaper Studio."
        } else {
            "The lock-screen wallpaper could not be updated: $error"
        }
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentTitle("VerseMoment update failed")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        manager.notify(UPDATE_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "VerseMomentAutomation"
        private const val NOTIFICATION_CHANNEL_ID = "wallpaper_automation"
        private const val UPDATE_NOTIFICATION_ID = 1002
    }
}
