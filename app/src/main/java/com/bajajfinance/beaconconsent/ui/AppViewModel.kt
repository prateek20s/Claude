package com.bajajfinance.beaconconsent.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bajajfinance.beaconconsent.bt.PeripheralReader
import com.bajajfinance.beaconconsent.data.*
import com.bajajfinance.beaconconsent.net.ServerClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

const val CONSENT_VERSION = "1.0"

enum class Screen { WELCOME, CONSENT, PERMISSION, DEVICES, SUCCESS, HOME, MANAGE }

data class UiState(
    val screen: Screen = Screen.WELCOME,
    val peripherals: List<Peripheral> = emptyList(),
    val registration: RegistrationState = RegistrationState.NOT_REGISTERED,
    val btPermissionGranted: Boolean = false,
    val btEnabled: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
    val mobileMasked: String = "",
    val registeredDevices: List<Peripheral> = emptyList()
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val reader = PeripheralReader(app)
    private val store = ConsentStore(app)
    private val server = ServerClient()

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val state = store.getRegistrationState()
            val regDevices = if (state == RegistrationState.REGISTERED)
                store.getRegisteredDevices() else emptyList()
            _ui.update {
                it.copy(
                    registration = state,
                    registeredDevices = regDevices,
                    screen = if (state == RegistrationState.REGISTERED) Screen.HOME else Screen.WELCOME
                )
            }
        }
    }

    fun go(screen: Screen) = _ui.update { it.copy(screen = screen, error = null) }

    fun refreshBtStatus() = _ui.update {
        it.copy(
            btPermissionGranted = reader.hasBluetoothPermission(),
            btEnabled = reader.isBluetoothOn()
        )
    }

    fun onPermissionResult(granted: Boolean) {
        _ui.update { it.copy(btPermissionGranted = granted) }
        if (granted) continueAfterPermission()
    }

    /**
     * Called after the "Nearby devices" permission is granted. If Bluetooth is
     * OFF, we cannot read paired devices, so we stay on the permission screen and
     * ask the user to turn it on. Only when BT is ON do we advance to the device list.
     */
    fun continueAfterPermission() {
        refreshBtStatus()
        if (!reader.isBluetoothOn()) {
            // Block: remain on permission screen, flag BT as off so UI shows the prompt.
            _ui.update { it.copy(btEnabled = false, screen = Screen.PERMISSION,
                error = null) }
            return
        }
        loadPeripheralsAndContinue()
    }

    /** User reports (or system confirms) BT is now on — re-verify and proceed or stay. */
    fun recheckBluetooth(): Boolean {
        refreshBtStatus()
        val on = reader.isBluetoothOn()
        if (on && reader.hasBluetoothPermission()) loadPeripheralsAndContinue()
        else _ui.update { it.copy(btEnabled = on) }
        return on
    }

    /** Fallback when the system enable dialog can't be shown (rare). Guide to settings. */
    fun onEnableBluetoothFailed() = _ui.update {
        it.copy(
            btEnabled = false,
            error = "Couldn't open the Bluetooth switch automatically. Please turn on " +
                "Bluetooth from your phone's settings or quick-settings panel, then come back."
        )
    }

    fun loadPeripheralsAndContinue() {
        refreshBtStatus()
        val list = reader.readPairedPeripherals()
        _ui.update { it.copy(peripherals = list, btEnabled = true, error = null, screen = Screen.DEVICES) }
    }

    fun toggleDevice(mac: String) = _ui.update { s ->
        s.copy(peripherals = s.peripherals.map {
            if (it.macAddress == mac) it.copy(selected = !it.selected) else it
        })
    }

    fun setMobile(masked: String) = _ui.update { it.copy(mobileMasked = masked) }

    fun register() {
        val selected = _ui.value.peripherals.filter { it.selected }
        if (selected.isEmpty()) {
            _ui.update { it.copy(error = "Select at least one device to register.") }
            return
        }
        _ui.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val consent = ConsentRecord(
                consentGiven = true,
                consentVersion = CONSENT_VERSION,
                consentTimestampUtc = Instant.now().toString(),
                purposesAccepted = listOf(
                    "in_store_identification",
                    "personalized_offers"
                )
            )
            store.saveConsent(consent)
            store.setRegistrationState(RegistrationState.IN_PROGRESS)

            val result = server.register(selected, consent, _ui.value.mobileMasked)
            if (result.ok) {
                result.userRef?.let { store.saveUserRef(it) }
                store.setRegistrationState(RegistrationState.REGISTERED)
                store.saveRegisteredDevices(selected)
                _ui.update {
                    it.copy(busy = false, registration = RegistrationState.REGISTERED,
                        registeredDevices = selected, screen = Screen.SUCCESS)
                }
            } else {
                store.setRegistrationState(RegistrationState.FAILED)
                _ui.update {
                    it.copy(busy = false, registration = RegistrationState.FAILED, error = result.message)
                }
            }
        }
    }

    /** Revoke consent for a SINGLE device. If it was the last one, consent ends fully. */
    fun revokeDevice(target: Peripheral) {
        _ui.update { it.copy(busy = true) }
        viewModelScope.launch {
            val ref = store.getUserRef()
            if (ref != null) server.revokeDevice(ref, target.macAddress)
            val remaining = _ui.value.registeredDevices.filterNot { it.macAddress == target.macAddress }
            if (remaining.isEmpty()) {
                // Last device removed -> treat as full withdrawal.
                store.clearAll()
                _ui.update { UiState(screen = Screen.WELCOME) }
            } else {
                store.saveRegisteredDevices(remaining)
                _ui.update {
                    it.copy(busy = false, registeredDevices = remaining, screen = Screen.MANAGE)
                }
            }
        }
    }

    fun withdrawConsent() {
        _ui.update { it.copy(busy = true) }
        viewModelScope.launch {
            val ref = store.getUserRef()
            if (ref != null) server.withdraw(ref)
            store.clearAll()
            _ui.update {
                UiState(screen = Screen.WELCOME) // reset everything
            }
        }
    }
}
