package com.versemoment.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "versemoment_prefs")

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val UPDATE_INTERVAL = intPreferencesKey("update_interval")
        val TIME_MATCH_ENABLED = booleanPreferencesKey("time_match_enabled")
        val DIVINE_DISCOVERY_ENABLED = booleanPreferencesKey("divine_discovery_enabled")
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val SELECTED_TRANSLATION = stringPreferencesKey("selected_translation")
        val SELECTED_THEME = stringPreferencesKey("selected_theme")
        val AUTOMATION_ENABLED = booleanPreferencesKey("automation_enabled")
    }

    val updateIntervalFlow: Flow<Int> = context.dataStore.data.map { it[Keys.UPDATE_INTERVAL] ?: 5 }
    val timeMatchEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.TIME_MATCH_ENABLED] ?: true }
    val divineDiscoveryEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.DIVINE_DISCOVERY_ENABLED] ?: true }
    val quietHoursEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.QUIET_HOURS_ENABLED] ?: false }
    val selectedTranslationFlow: Flow<String> = context.dataStore.data.map { it[Keys.SELECTED_TRANSLATION] ?: "kjv" }
    val selectedThemeFlow: Flow<String> = context.dataStore.data.map { it[Keys.SELECTED_THEME] ?: "cinematic_nature" }
    val automationEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTOMATION_ENABLED] ?: true }

    suspend fun setUpdateInterval(value: Int) {
        context.dataStore.edit { it[Keys.UPDATE_INTERVAL] = value }
    }

    suspend fun setTimeMatchEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.TIME_MATCH_ENABLED] = value }
    }

    suspend fun setDivineDiscoveryEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.DIVINE_DISCOVERY_ENABLED] = value }
    }

    suspend fun setQuietHoursEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.QUIET_HOURS_ENABLED] = value }
    }

    suspend fun setSelectedTheme(value: String) {
        context.dataStore.edit { it[Keys.SELECTED_THEME] = value }
    }

    suspend fun setSelectedTranslation(value: String) {
        context.dataStore.edit { it[Keys.SELECTED_TRANSLATION] = value }
    }

    suspend fun setAutomationEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.AUTOMATION_ENABLED] = value }
    }
}
