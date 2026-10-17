package com.versemoment.app

import android.app.Application
import android.util.Log
import com.versemoment.app.data.PreferencesRepository
import com.versemoment.app.scheduling.AutomationScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltAndroidApp
class VerseMomentApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val preferences = PreferencesRepository(this@VerseMomentApplication)
                val scheduler = AutomationScheduler(this@VerseMomentApplication)
                if (preferences.automationEnabledFlow.first()) {
                    scheduler.scheduleNextUpdate(preferences.updateIntervalFlow.first())
                } else {
                    scheduler.cancel()
                }
            } catch (exception: Exception) {
                Log.e("VerseMomentAutomation", "Could not initialize wallpaper automation", exception)
            }
        }
    }
}
