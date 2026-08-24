package com.asiradnan.asirtasks.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

interface UserPreferencesManager {
    val lastSyncTime: Flow<Long?>
    val isDarkMode: Flow<Boolean?>
    suspend fun saveLastSyncTime(time: Long)
    suspend fun setTheme(isDark: Boolean)
}

class DataStoreUserPreferencesManager(private val context: Context) : UserPreferencesManager {
    private val lastSyncTimeKey = longPreferencesKey("last_sync_time")
    private val isDarkModeKey = booleanPreferencesKey("is_dark_mode")

    override val lastSyncTime: Flow<Long?> = context.dataStore.data.map { it[lastSyncTimeKey] }
    override val isDarkMode: Flow<Boolean?> = context.dataStore.data.map { it[isDarkModeKey] }

    override suspend fun saveLastSyncTime(time: Long) {
        context.dataStore.edit { prefs -> prefs[lastSyncTimeKey] = time }
    }

    override suspend fun setTheme(isDark: Boolean) {
        context.dataStore.edit { prefs -> prefs[isDarkModeKey] = isDark }
    }
}
