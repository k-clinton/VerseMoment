package com.versemoment.app.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.versemoment.app.receivers.BootReceiver

class AutomationScheduler(private val context: Context) {
    fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return requireNotNull(context.getSystemService(AlarmManager::class.java)).canScheduleExactAlarms()
    }

    fun scheduleNextUpdate(intervalMinutes: Int, requestCode: Int = 1001) {
        val alarmManager = requireNotNull(context.getSystemService(AlarmManager::class.java))
        val pendingIntent = updatePendingIntent(requestCode)
        val triggerAtMillis = System.currentTimeMillis() + intervalMinutes.coerceAtLeast(1) * 60_000L
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            } else {
                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    60_000L,
                    pendingIntent,
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                60_000L,
                pendingIntent,
            )
        }
    }

    fun cancel(requestCode: Int = 1001) {
        val alarmManager = requireNotNull(context.getSystemService(AlarmManager::class.java))
        alarmManager.cancel(updatePendingIntent(requestCode))
    }

    private fun updatePendingIntent(requestCode: Int) = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, BootReceiver::class.java).apply {
            action = ACTION_UPDATE_WALLPAPER
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val ACTION_UPDATE_WALLPAPER = "com.versemoment.app.UPDATE_WALLPAPER"
    }
}
