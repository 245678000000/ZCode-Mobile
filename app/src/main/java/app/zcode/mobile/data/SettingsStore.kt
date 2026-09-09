package app.zcode.mobile.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "zcode_settings")

data class AppSettings(
    val voiceEnabled: Boolean = true,
    val taskNotifications: Boolean = true,
    val approvalNotifications: Boolean = true,
    val allowDownloads: Boolean = true,
    val allowExternalLinks: Boolean = true,
)

class SettingsStore(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            voiceEnabled = prefs[Keys.VOICE] ?: true,
            taskNotifications = prefs[Keys.TASK_NOTIFICATIONS] ?: true,
            approvalNotifications = prefs[Keys.APPROVAL_NOTIFICATIONS] ?: true,
            allowDownloads = prefs[Keys.ALLOW_DOWNLOADS] ?: true,
            allowExternalLinks = prefs[Keys.ALLOW_EXTERNAL] ?: true,
        )
    }

    suspend fun setVoiceEnabled(value: Boolean) = set(Keys.VOICE, value)
    suspend fun setTaskNotifications(value: Boolean) = set(Keys.TASK_NOTIFICATIONS, value)
    suspend fun setApprovalNotifications(value: Boolean) = set(Keys.APPROVAL_NOTIFICATIONS, value)
    suspend fun setAllowDownloads(value: Boolean) = set(Keys.ALLOW_DOWNLOADS, value)
    suspend fun setAllowExternalLinks(value: Boolean) = set(Keys.ALLOW_EXTERNAL, value)

    private suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { it[key] = value }
    }

    private object Keys {
        val VOICE = booleanPreferencesKey("voice_enabled")
        val TASK_NOTIFICATIONS = booleanPreferencesKey("task_notifications")
        val APPROVAL_NOTIFICATIONS = booleanPreferencesKey("approval_notifications")
        val ALLOW_DOWNLOADS = booleanPreferencesKey("allow_downloads")
        val ALLOW_EXTERNAL = booleanPreferencesKey("allow_external_links")
    }
}
