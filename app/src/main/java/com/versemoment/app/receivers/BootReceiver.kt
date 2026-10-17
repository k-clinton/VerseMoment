package com.versemoment.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.AlarmManager
import android.util.Log
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

                val database = VerseMomentDatabase.getInstance(appContext)
                val repository = VerseRepository(database.verseDao())
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

    companion object {
        private const val TAG = "VerseMomentAutomation"
    }
}
