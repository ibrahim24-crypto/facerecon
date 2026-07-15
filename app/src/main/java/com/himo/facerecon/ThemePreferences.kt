package com.himo.facerecon

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_preferences"
)

class ThemePreferences(private val context: Context) {

    private val darkThemeKey = booleanPreferencesKey("pref_dark_theme")
    private val showFaceIndexKey = booleanPreferencesKey("pref_show_face_index")
    private val devDisableSubmitKey = booleanPreferencesKey("pref_dev_disable_submit")
    private val developerNameKey = androidx.datastore.preferences.core.stringPreferencesKey("pref_developer_name")
    private val autoCaptureEnabledKey = booleanPreferencesKey("pref_auto_capture_enabled")
    private val autoCaptureIntervalKey = androidx.datastore.preferences.core.intPreferencesKey("pref_auto_capture_interval_s")
    private val languageKey = androidx.datastore.preferences.core.stringPreferencesKey("pref_language")

    suspend fun readDarkTheme(): Boolean? {
        return context.themeDataStore.data
            .map { prefs -> prefs[darkThemeKey] }
            .first()
    }

    suspend fun saveDarkTheme(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[darkThemeKey] = enabled
        }
    }

    suspend fun readShowFaceIndex(): Boolean? {
        return context.themeDataStore.data
            .map { prefs -> prefs[showFaceIndexKey] }
            .first()
    }

    suspend fun saveShowFaceIndex(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[showFaceIndexKey] = enabled
        }
    }

    suspend fun readDevDisableSubmit(): Boolean? {
        return context.themeDataStore.data
            .map { prefs -> prefs[devDisableSubmitKey] }
            .first()
    }

    suspend fun saveDevDisableSubmit(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[devDisableSubmitKey] = enabled
        }
    }

    suspend fun readDeveloperName(): String? {
        return context.themeDataStore.data
            .map { prefs -> prefs[developerNameKey] }
            .first()
    }

    suspend fun saveDeveloperName(name: String) {
        context.themeDataStore.edit { prefs ->
            prefs[developerNameKey] = name
        }
    }

    suspend fun readAutoCaptureEnabled(): Boolean? {
        return context.themeDataStore.data.map { prefs -> prefs[autoCaptureEnabledKey] }.first()
    }

    suspend fun saveAutoCaptureEnabled(enabled: Boolean) {
        context.themeDataStore.edit { prefs -> prefs[autoCaptureEnabledKey] = enabled }
    }

    suspend fun readAutoCaptureInterval(): Int? {
        return context.themeDataStore.data.map { prefs -> prefs[autoCaptureIntervalKey] }.first()
    }

    suspend fun saveAutoCaptureInterval(seconds: Int) {
        context.themeDataStore.edit { prefs -> prefs[autoCaptureIntervalKey] = seconds }
    }

    suspend fun readLanguage(): String? {
        return context.themeDataStore.data.map { prefs -> prefs[languageKey] }.first()
    }

    suspend fun saveLanguage(lang: String) {
        context.themeDataStore.edit { prefs -> prefs[languageKey] = lang }
    }
}
