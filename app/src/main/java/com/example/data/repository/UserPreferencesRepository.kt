package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.simPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "vaultplay_ui_prefs")

enum class LocalAccountMode {
    GUEST,
    PHONE
}

data class AccountSessionPrefs(
    val mode: LocalAccountMode = LocalAccountMode.GUEST,
    val phoneNumber: String = "",
    val phoneDisplayName: String = ""
) {
    val localAccountKey: String
        get() = if (mode == LocalAccountMode.PHONE && phoneNumber.isNotBlank()) {
            "phone_${phoneNumber.filter(Char::isDigit).take(15)}"
        } else {
            "guest_default"
        }

    val effectiveDisplayName: String
        get() = if (mode == LocalAccountMode.PHONE && phoneNumber.isNotBlank()) {
            phoneDisplayName.ifBlank { "Member $phoneNumber" }
        } else {
            "Guest Simulator"
        }
}

class UserPreferencesRepository(private val context: Context) {
    private val darkModeKey = booleanPreferencesKey("dark_mode_enabled")
    private val accountModeKey = stringPreferencesKey("local_account_mode")
    private val phoneNumberKey = stringPreferencesKey("local_phone_number")
    private val phoneNameKey = stringPreferencesKey("local_phone_name")

    val darkModeFlow: Flow<Boolean> = context.simPrefsDataStore.data.map { prefs ->
        prefs[darkModeKey] ?: true
    }

    val accountSessionFlow: Flow<AccountSessionPrefs> = context.simPrefsDataStore.data.map { prefs ->
        val mode = if (prefs[accountModeKey] == LocalAccountMode.PHONE.name) {
            LocalAccountMode.PHONE
        } else {
            LocalAccountMode.GUEST
        }
        AccountSessionPrefs(
            mode = mode,
            phoneNumber = prefs[phoneNumberKey] ?: "",
            phoneDisplayName = prefs[phoneNameKey] ?: ""
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.simPrefsDataStore.edit { prefs ->
            prefs[darkModeKey] = enabled
        }
    }

    suspend fun setPhoneProfile(phoneNumber: String, displayName: String) {
        val cleanDigits = phoneNumber.filter { it.isDigit() || it == '+' }.take(16)
        val cleanName = displayName.trim().take(40)
        context.simPrefsDataStore.edit { prefs ->
            prefs[accountModeKey] = LocalAccountMode.PHONE.name
            prefs[phoneNumberKey] = cleanDigits
            prefs[phoneNameKey] = cleanName
        }
    }

    suspend fun switchToGuestMode() {
        context.simPrefsDataStore.edit { prefs ->
            prefs[accountModeKey] = LocalAccountMode.GUEST.name
        }
    }
}
