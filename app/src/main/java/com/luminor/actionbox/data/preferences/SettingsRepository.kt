package com.luminor.actionbox.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.luminor.actionbox.domain.UiSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "actionbox_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val replyTone = stringPreferencesKey("reply_tone")
        val haptics = booleanPreferencesKey("haptics")
    }

    val settings: Flow<UiSettings> = context.dataStore.data.map { prefs ->
        UiSettings(
            themeMode = prefs[Keys.theme] ?: "SYSTEM",
            replyTone = prefs[Keys.replyTone] ?: "PROFESSIONAL",
            hapticsEnabled = prefs[Keys.haptics] ?: true
        )
    }

    suspend fun setTheme(value: String) {
        context.dataStore.edit { it[Keys.theme] = value }
    }

    suspend fun setReplyTone(value: String) {
        context.dataStore.edit { it[Keys.replyTone] = value }
    }

    suspend fun setHaptics(value: Boolean) {
        context.dataStore.edit { it[Keys.haptics] = value }
    }
}
