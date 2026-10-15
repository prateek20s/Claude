package com.bajajfinance.beaconconsent.data

/**
 * A Bluetooth peripheral that is ALREADY PAIRED to this phone
 * (watch, earbuds, fitness band, etc.). We only ever read bonded devices.
 */
data class Peripheral(
    val name: String,          // e.g. "Galaxy Watch6", "boAt Airdopes"
    val macAddress: String,    // e.g. "A4:C3:xx:xx:9F:12"  (Bluetooth Classic MAC)
    val type: String,          // Phone / Watch / Earbuds / Other
    var selected: Boolean = true
)

/** Status of the one-time registration with the Bajaj/EasyReach server. */
enum class RegistrationState { NOT_REGISTERED, IN_PROGRESS, REGISTERED, FAILED }

/** The consent record we store locally and send with every registration. */
data class ConsentRecord(
    val consentGiven: Boolean,
    val consentVersion: String,     // bump when the DPDP notice text changes
    val consentTimestampUtc: String,
    val purposesAccepted: List<String>
)
