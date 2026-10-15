package com.bajajfinance.beaconconsent.bt

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.bajajfinance.beaconconsent.data.Peripheral

/**
 * Reads the phone's ALREADY-PAIRED (bonded) Bluetooth peripherals.
 *
 * IMPORTANT TECHNICAL NOTE:
 *  - We do NOT and CANNOT read the phone's own Bluetooth MAC (OS-blocked since
 *    Android 6, returns a fixed dummy 02:00:00:00:00:00). That is by design and
 *    is exactly why this flow registers PERIPHERAL MACs instead.
 *  - We do NOT scan the air for nearby devices (that needs location permission
 *    and would raise the DPDP footprint). We only enumerate bonded devices,
 *    which the beacon can later match against.
 */
class PeripheralReader(private val context: Context) {

    fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // legacy perms are install-time on <12
        }
    }

    fun isBluetoothOn(): Boolean {
        val mgr = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return mgr?.adapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission") // guarded by hasBluetoothPermission() at call site
    fun readPairedPeripherals(): List<Peripheral> {
        if (!hasBluetoothPermission()) return emptyList()
        val mgr = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = mgr?.adapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()

        return adapter.bondedDevices
            .orEmpty()
            .map { device ->
                Peripheral(
                    name = safeName(device),
                    macAddress = device.address ?: "Unknown",
                    type = classify(device)
                )
            }
            .sortedBy { it.type }
    }

    @SuppressLint("MissingPermission")
    private fun safeName(device: BluetoothDevice): String =
        device.name?.takeIf { it.isNotBlank() } ?: "Unnamed device"

    @SuppressLint("MissingPermission")
    private fun classify(device: BluetoothDevice): String {
        val major = device.bluetoothClass?.majorDeviceClass
        val dc = device.bluetoothClass?.deviceClass
        return when {
            dc == BluetoothClass.Device.WEARABLE_WRIST_WATCH -> "Watch"
            major == BluetoothClass.Device.Major.WEARABLE -> "Watch"
            major == BluetoothClass.Device.Major.AUDIO_VIDEO -> "Earbuds"
            major == BluetoothClass.Device.Major.PHONE -> "Phone"
            else -> "Other"
        }
    }
}
