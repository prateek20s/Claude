package com.bajajfinance.beaconconsent

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
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

    // When the enable-Bluetooth request has to pause to collect the BLUETOOTH_CONNECT
    // permission first, we flip this on so the permission callback can resume it.
    private var pendingEnableAfterPermission = false

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            vm.onPermissionResult(granted)
            if (granted && pendingEnableAfterPermission) {
                pendingEnableAfterPermission = false
                // Permission is now held, so it's safe to show the system enable dialog.
                if (!vm.ui.value.btEnabled) launchEnableBtIntent()
            } else {
                pendingEnableAfterPermission = false
            }
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
                        error = ui.error,
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
        // On Android 12+ (API 31) the ACTION_REQUEST_ENABLE intent REQUIRES the
        // BLUETOOTH_CONNECT runtime permission. If we launch it without the permission
        // the system throws a SecurityException and the app crashes. So if BT is off and
        // we don't yet hold the permission, grab the permission first and resume the
        // enable request from the permission callback.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            pendingEnableAfterPermission = true
            permLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            return
        }
        launchEnableBtIntent()
    }

    /** Launches the system "Turn on Bluetooth?" dialog, guarded against SecurityException. */
    private fun launchEnableBtIntent() {
        try {
            enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } catch (e: SecurityException) {
            vm.onEnableBluetoothFailed()
        }
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
