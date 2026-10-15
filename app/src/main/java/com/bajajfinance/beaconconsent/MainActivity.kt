package com.bajajfinance.beaconconsent

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bajajfinance.beaconconsent.ui.*
import com.bajajfinance.beaconconsent.ui.theme.BeaconConsentTheme

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            vm.onPermissionResult(granted)
        }

    // System "Turn on Bluetooth?" dialog. On return we re-check actual BT state.
    private val enableBtLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            vm.recheckBluetooth()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BeaconConsentTheme {
                val ui by vm.ui.collectAsStateWithLifecycle()
                when (ui.screen) {
                    Screen.WELCOME -> WelcomeScreen(onStart = { vm.go(Screen.CONSENT) })

                    Screen.CONSENT -> ConsentScreen(
                        onAgree = {
                            // After DPDP consent, go to the permission primer. The gate
                            // (permission + Bluetooth-on) is enforced from there.
                            vm.refreshBtStatus()
                            if (vm.ui.value.btPermissionGranted) {
                                // Permission already held — still verify Bluetooth is ON.
                                vm.continueAfterPermission()
                            } else {
                                vm.go(Screen.PERMISSION)
                            }
                        },
                        onBack = { vm.go(Screen.WELCOME) }
                    )

                    Screen.PERMISSION -> PermissionScreen(
                        btEnabled = ui.btEnabled,
                        onRequest = { requestBluetooth() },
                        onEnableBluetooth = { requestEnableBluetooth() },
                        onBack = { vm.go(Screen.CONSENT) }
                    )

                    Screen.DEVICES -> DevicesScreen(
                        peripherals = ui.peripherals,
                        busy = ui.busy,
                        error = ui.error,
                        btEnabled = ui.btEnabled,
                        onToggle = vm::toggleDevice,
                        onRegister = vm::register,
                        onBack = { vm.go(Screen.CONSENT) }
                    )

                    Screen.SUCCESS -> SuccessScreen(onHome = { vm.go(Screen.HOME) })

                    Screen.HOME -> HomeScreen(
                        devices = ui.registeredDevices,
                        onManage = { vm.go(Screen.MANAGE) }
                    )

                    Screen.MANAGE -> ManageScreen(
                        registration = ui.registration,
                        devices = ui.registeredDevices,
                        busy = ui.busy,
                        onRevokeDevice = vm::revokeDevice,
                        onWithdraw = vm::withdrawConsent,
                        onBack = { vm.go(Screen.HOME) }
                    )
                }
            }
        }
    }

    private fun requestBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            // Legacy devices: permission is install-time, proceed directly.
            vm.onPermissionResult(true)
        }
    }

    /** Ask the system to turn Bluetooth on (shows Android's own enable dialog). */
    private fun requestEnableBluetooth() {
        enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }

    override fun onResume() {
        super.onResume()
        // If the user toggled Bluetooth from the quick-settings panel and came back,
        // re-check so the gate clears (or re-appears) without a restart.
        vm.refreshBtStatus()
        if (vm.ui.value.screen == Screen.PERMISSION && vm.ui.value.btPermissionGranted) {
            vm.recheckBluetooth()
        }
    }
}
