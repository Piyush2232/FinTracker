package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fintracker_prefs")

data class UserPrefs(
    val userDisplayName: String = "Local Profile",
    val lastPromptedMonth: String = "",
    val smsAutoSyncEnabled: Boolean = true,
    val lastSmsScanTimestamp: Long = 0L,
    val lastScanInspected: Int = 0,
    val lastScanMatched: Int = 0,
    val lastScanSkipped: Int = 0,
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val USER_NAME = stringPreferencesKey("user_display_name")
        val LAST_PROMPTED_MONTH = stringPreferencesKey("last_prompted_month")
        val SMS_AUTO_SYNC = booleanPreferencesKey("sms_auto_sync")
        val LAST_SMS_SCAN_TS = longPreferencesKey("last_sms_scan_ts")
        val LAST_SCAN_INSPECTED = intPreferencesKey("last_scan_inspected")
        val LAST_SCAN_MATCHED = intPreferencesKey("last_scan_matched")
        val LAST_SCAN_SKIPPED = intPreferencesKey("last_scan_skipped")
    }

    val prefsFlow: Flow<UserPrefs> = context.dataStore.data.map { prefs ->
        UserPrefs(
            userDisplayName = prefs[Keys.USER_NAME] ?: "Local Profile",
            lastPromptedMonth = prefs[Keys.LAST_PROMPTED_MONTH] ?: "",
            smsAutoSyncEnabled = prefs[Keys.SMS_AUTO_SYNC] ?: true,
            lastSmsScanTimestamp = prefs[Keys.LAST_SMS_SCAN_TS] ?: 0L,
            lastScanInspected = prefs[Keys.LAST_SCAN_INSPECTED] ?: 0,
            lastScanMatched = prefs[Keys.LAST_SCAN_MATCHED] ?: 0,
            lastScanSkipped = prefs[Keys.LAST_SCAN_SKIPPED] ?: 0,
        )
    }

    suspend fun setLastPromptedMonth(monthKey: String) {
        context.dataStore.edit { it[Keys.LAST_PROMPTED_MONTH] = monthKey }
    }

    suspend fun setUserDisplayName(name: String) {
        context.dataStore.edit { it[Keys.USER_NAME] = name.trim().ifEmpty { "Local Profile" } }
    }

    suspend fun setSmsAutoSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SMS_AUTO_SYNC] = enabled }
    }

    suspend fun recordSmsScanDiagnostics(
        timestamp: Long,
        inspected: Int,
        matched: Int,
        skipped: Int,
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SMS_SCAN_TS] = timestamp
            prefs[Keys.LAST_SCAN_INSPECTED] = inspected
            prefs[Keys.LAST_SCAN_MATCHED] = matched
            prefs[Keys.LAST_SCAN_SKIPPED] = skipped
        }
    }
}
