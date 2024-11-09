package com.thiarara.myapplicatio.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val SHOW_GRID = booleanPreferencesKey("show_grid")
        val SHOW_TIPS = booleanPreferencesKey("show_tips")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    }

    val geminiApiKey: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[GEMINI_API_KEY]
    }

    val showGrid: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_GRID] ?: true // Default to true
    }

    val showTips: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_TIPS] ?: true // Default to true
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_DARK_MODE] ?: false // Default to light mode
    }

    suspend fun saveGeminiApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[GEMINI_API_KEY] = apiKey
        }
    }

    suspend fun saveShowGrid(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_GRID] = show
        }
    }

    suspend fun saveShowTips(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_TIPS] = show
        }
    }

    suspend fun saveIsDarkMode(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_MODE] = isDark
        }
    }
} 