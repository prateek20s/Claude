package com.bajajfinance.beaconconsent.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "beacon_consent")

/** Persists consent + registration status locally so the user can withdraw later. */
class ConsentStore(private val context: Context) {

    private object Keys {
        val CONSENT = booleanPreferencesKey("consent_given")
        val VERSION = stringPreferencesKey("consent_version")
        val TS = stringPreferencesKey("consent_ts")
        val STATE = stringPreferencesKey("reg_state")
        val USER_REF = stringPreferencesKey("user_ref")
        val REG_DEVICES = stringPreferencesKey("reg_devices")  // "label|type|mac" per line
    }

    suspend fun saveConsent(record: ConsentRecord) {
        context.dataStore.edit {
            it[Keys.CONSENT] = record.consentGiven
            it[Keys.VERSION] = record.consentVersion
            it[Keys.TS] = record.consentTimestampUtc
        }
    }

    suspend fun setRegistrationState(state: RegistrationState) {
        context.dataStore.edit { it[Keys.STATE] = state.name }
    }

    suspend fun saveUserRef(ref: String) {
        context.dataStore.edit { it[Keys.USER_REF] = ref }
    }

    suspend fun getUserRef(): String? =
        context.dataStore.data.first()[Keys.USER_REF]

    suspend fun getRegistrationState(): RegistrationState =
        runCatching {
            RegistrationState.valueOf(
                context.dataStore.data.first()[Keys.STATE] ?: RegistrationState.NOT_REGISTERED.name
            )
        }.getOrDefault(RegistrationState.NOT_REGISTERED)

    suspend fun hasConsent(): Boolean =
        context.dataStore.data.first()[Keys.CONSENT] ?: false

    /** Persist the registered devices (display info) so the Home screen can show them. */
    suspend fun saveRegisteredDevices(devices: List<Peripheral>) {
        val blob = devices.joinToString("\n") { "${it.name}|${it.type}|${it.macAddress}" }
        context.dataStore.edit { it[Keys.REG_DEVICES] = blob }
    }

    suspend fun getRegisteredDevices(): List<Peripheral> {
        val blob = context.dataStore.data.first()[Keys.REG_DEVICES].orEmpty()
        if (blob.isBlank()) return emptyList()
        return blob.lines().mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size == 3) Peripheral(parts[0], parts[2], parts[1], selected = true) else null
        }
    }

    /** Full local wipe — used when the user withdraws consent. */
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
