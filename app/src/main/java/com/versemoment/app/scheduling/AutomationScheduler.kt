package com.versemoment.app.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.versemoment.app.receivers.BootReceiver
import java.util.Calendar

class AutomationScheduler(private val context: Context) {
    fun scheduleNextUpdate(intervalMinutes: Int, requestCode: Int = 1001) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = "com.versemoment.app.UPDATE_WALLPAPER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, intervalMinutes.coerceAtLeast(1))
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent,
                )
            } else {
                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    60_000L,
                    pendingIntent,
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                60_000L,
                pendingIntent,
            )
        }
    }
}
