package com.seigz.webjingles.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cocoon_jingles_settings")

class AppPreferences(private val context: Context) {

    companion object {
        val PREFERRED_FORMAT = stringPreferencesKey("preferred_format")
        val DOWNLOAD_FOLDER_URI = stringPreferencesKey("download_folder_uri")
        val DOWNLOAD_FOLDER_NAME = stringPreferencesKey("download_folder_name")
        val ENABLE_PORTRAIT = booleanPreferencesKey("enable_portrait")
        val AUTO_DOWNLOAD_HQ = booleanPreferencesKey("auto_download_hq")
        val NORMALIZE_AUDIO = booleanPreferencesKey("normalize_audio")
        val YOUTUBE_API_KEY = stringPreferencesKey("youtube_api_key")
        val RECENT_SEARCHES = stringSetPreferencesKey("recent_searches")
        val FAVORITES_JSON = stringPreferencesKey("favorites_json")
        val CACHE_SIZE_BYTES = longPreferencesKey("cache_size_bytes")
        val FULLSCREEN_MODE = booleanPreferencesKey("fullscreen_mode")
        val UI_SCALE = floatPreferencesKey("ui_scale")
        val ADVANCED_MODE = booleanPreferencesKey("advanced_mode")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val BETTER_SEARCHING = booleanPreferencesKey("better_searching")
    }

    val preferredFormat: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PREFERRED_FORMAT] ?: "WAV"
    }

    val downloadFolderUri: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[DOWNLOAD_FOLDER_URI]
    }

    val downloadFolderName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[DOWNLOAD_FOLDER_NAME] ?: "/Music/WebJingles/"
    }

    val enablePortrait: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ENABLE_PORTRAIT] ?: false
    }

    val autoDownloadHQ: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTO_DOWNLOAD_HQ] ?: false
    }

    val normalizeAudio: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[NORMALIZE_AUDIO] ?: false
    }

    val youtubeApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[YOUTUBE_API_KEY] ?: ""
    }

    val recentSearches: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[RECENT_SEARCHES] ?: emptySet()
    }

    @Suppress("unused")
    val favoritesJson: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[FAVORITES_JSON] ?: "[]"
    }

    val fullscreenMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[FULLSCREEN_MODE] ?: true
    }

    val uiScale: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[UI_SCALE] ?: 1.0f
    }

    val advancedMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ADVANCED_MODE] ?: false
    }

    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SOUND_ENABLED] ?: true
    }

    val betterSearching: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[BETTER_SEARCHING] ?: true
    }

    suspend fun setPreferredFormat(format: String) {
        context.dataStore.edit { prefs -> prefs[PREFERRED_FORMAT] = format }
    }

    suspend fun setDownloadFolder(uri: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[DOWNLOAD_FOLDER_URI] = uri
            prefs[DOWNLOAD_FOLDER_NAME] = name
        }
    }

    suspend fun setEnablePortrait(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[ENABLE_PORTRAIT] = enabled }
    }

    suspend fun setAutoDownloadHQ(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[AUTO_DOWNLOAD_HQ] = enabled }
    }

    suspend fun setNormalizeAudio(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[NORMALIZE_AUDIO] = enabled }
    }

    suspend fun setYoutubeApiKey(key: String) {
        context.dataStore.edit { prefs -> prefs[YOUTUBE_API_KEY] = key }
    }

    suspend fun addRecentSearch(query: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[RECENT_SEARCHES]?.toMutableSet() ?: mutableSetOf()
            current.add(query)
            if (current.size > 20) {
                val trimmed = current.toList().takeLast(20).toSet()
                prefs[RECENT_SEARCHES] = trimmed
            } else {
                prefs[RECENT_SEARCHES] = current
            }
        }
    }

    suspend fun clearRecentSearches() {
        context.dataStore.edit { prefs -> prefs[RECENT_SEARCHES] = emptySet() }
    }

    @Suppress("unused")
    suspend fun setFavoritesJson(json: String) {
        context.dataStore.edit { prefs -> prefs[FAVORITES_JSON] = json }
    }

    suspend fun clearCache() {
        context.dataStore.edit { prefs -> prefs[CACHE_SIZE_BYTES] = 0L }
    }

    suspend fun setFullscreenMode(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[FULLSCREEN_MODE] = enabled }
    }

    suspend fun setUiScale(scale: Float) {
        context.dataStore.edit { prefs -> prefs[UI_SCALE] = scale.coerceIn(0.5f, 2.0f) }
    }

    suspend fun setAdvancedMode(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[ADVANCED_MODE] = enabled }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[SOUND_ENABLED] = enabled }
    }

    suspend fun setBetterSearching(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[BETTER_SEARCHING] = enabled }
    }
}
