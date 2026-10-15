package com.bajajfinance.beaconconsent.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.bajajfinance.beaconconsent.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bajajfinance.beaconconsent.data.Peripheral
import com.bajajfinance.beaconconsent.data.RegistrationState
import com.bajajfinance.beaconconsent.ui.theme.*

/* ---------- shared chrome ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Header(title: String) {
    Surface(color = BflNavyBand) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("PERSONALIZED OFFERS", color = BflGold, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(2.dp))
                Text(title, color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            // Official Bajaj Finserv logo in a white chip (brand mark reads only on light)
            Surface(color = androidx.compose.ui.graphics.Color.White,
                shape = RoundedCornerShape(6.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.bfl_logo),
                    contentDescription = "Bajaj Finserv",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp).height(22.dp)
                )
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BflNavy)
    ) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun BulletRow(icon: ImageVector, tint: androidx.compose.ui.graphics.Color, text: String) {
    Row(Modifier.padding(vertical = 5.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp).padding(top = 1.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

/* ---------- 1. Welcome ---------- */

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    Scaffold(
        topBar = { Header("Personalized Offers") },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                    PrimaryButton("Get started", onClick = onStart)
                }
            }
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Icon(Icons.Filled.Bluetooth, null, tint = BflCyan, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(20.dp))
            Text("Get recognised. Get better offers.",
                fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(
                "Register your paired Bluetooth devices (like your watch or earbuds) so " +
                    "Bajaj Finance can recognise you at partner outlets and serve you faster, " +
                    "relevant finance offers.",
                fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(24.dp))
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    BulletRow(Icons.Outlined.LockClock, BflBlue, "Fully optional — withdraw anytime")
                    BulletRow(Icons.Outlined.LocationOff, BflBlue, "No location tracking")
                    BulletRow(Icons.Outlined.Shield, BflBlue, "Device IDs stored only as a secure hash")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/* ---------- 2. Consent (DPDP) ---------- */

@Composable
fun ConsentScreen(onAgree: () -> Unit, onBack: () -> Unit) {
    var checked by remember { mutableStateOf(false) }
    Scaffold(topBar = { Header(ConsentText.TITLE) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp)
            ) {
                Text(ConsentText.INTRO, fontSize = 15.sp, lineHeight = 22.sp)
                Spacer(Modifier.height(20.dp))

                SectionTitle("What we collect")
                ConsentText.WHAT_WE_COLLECT.forEach {
                    BulletRow(Icons.Outlined.CheckCircle, BflBlue, it)
                }
                Spacer(Modifier.height(16.dp))

                SectionTitle("What we never do")
                ConsentText.WHAT_WE_DONT.forEach {
                    BulletRow(Icons.Outlined.Block, BflNavy, it)
                }
                Spacer(Modifier.height(16.dp))

                SectionTitle("Why we use it")
                ConsentText.PURPOSE.forEach {
                    BulletRow(Icons.Outlined.Campaign, BflBlue, it)
                }
                Spacer(Modifier.height(16.dp))

                SectionTitle("How long we keep it")
                Text(ConsentText.RETENTION, fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(Modifier.height(16.dp))

                SectionTitle("Your rights")
                ConsentText.RIGHTS.forEach {
                    BulletRow(Icons.Outlined.Verified, BflBlue, it)
                }
                Spacer(Modifier.height(10.dp))
                Text(ConsentText.CONTACT, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    color = BflBlue)
                Spacer(Modifier.height(20.dp))

                Row(verticalAlignment = Alignment.Top) {
                    Checkbox(checked = checked, onCheckedChange = { checked = it })
                    Spacer(Modifier.width(4.dp))
                    Text(ConsentText.CHECKBOX_LABEL, fontSize = 14.sp, lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 12.dp))
                }
            }
            Surface(shadowElevation = 8.dp) {
                Column(Modifier.padding(16.dp)) {
                    PrimaryButton("I agree & continue", enabled = checked, onClick = onAgree)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                        Text("Not now")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(t: String) {
    Text(t, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BflNavy,
        letterSpacing = 0.5.sp)
    Spacer(Modifier.height(6.dp))
}

/* ---------- 3. Permission ---------- */

@Composable
fun PermissionScreen(
    btEnabled: Boolean,
    error: String? = null,
    onRequest: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = { Header("Bluetooth permission") },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                    if (!btEnabled) {
                        PrimaryButton("Turn on Bluetooth", onClick = onEnableBluetooth)
                    } else {
                        PrimaryButton("Allow Bluetooth access", onClick = onRequest)
                    }
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
                }
            }
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            if (!btEnabled) {
                // ---- Bluetooth is OFF: block here until the user turns it on ----
                Icon(Icons.Filled.BluetoothDisabled, null, tint = BflGold,
                    modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(20.dp))
                Text("Turn on Bluetooth to continue",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Bluetooth is currently off. We need it on to read the devices paired to " +
                        "your phone. Tap below to turn it on — you can't continue until it's on.",
                    fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            } else {
                Icon(Icons.Filled.BluetoothSearching, null, tint = BflCyan,
                    modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(20.dp))
                Text("Allow access to nearby devices",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Android will now ask for the \"Nearby devices\" permission. We use it only " +
                        "to read the list of devices already paired to your phone — so you can " +
                        "pick which ones to register. We do not scan the area or use your location.",
                    fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }
            if (error != null) {
                Spacer(Modifier.height(16.dp))
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 14.sp,
                    textAlign = TextAlign.Center, lineHeight = 20.sp)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/* ---------- 4. Device selection ---------- */

@Composable
fun DevicesScreen(
    peripherals: List<Peripheral>,
    busy: Boolean,
    error: String?,
    btEnabled: Boolean,
    onToggle: (String) -> Unit,
    onRegister: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(topBar = { Header("Choose devices to register") }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
                if (!btEnabled) {
                    AssistCard(Icons.Outlined.BluetoothDisabled,
                        "Bluetooth is off. Turn it on, then reopen this screen to see your paired devices.")
                    Spacer(Modifier.height(12.dp))
                }
                if (peripherals.isEmpty()) {
                    AssistCard(Icons.Outlined.Info,
                        "No paired devices found. Pair your watch or earbuds in your phone's " +
                            "Bluetooth settings first, then come back.")
                } else {
                    Text("These are the devices already paired to your phone. " +
                        "Select the ones you carry regularly.",
                        fontSize = 14.sp, lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Spacer(Modifier.height(16.dp))
                    peripherals.forEach { p -> DeviceRow(p, onToggle) }
                }
                error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }
            }
            Surface(shadowElevation = 8.dp) {
                Column(Modifier.padding(16.dp)) {
                    if (busy) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                    }
                    PrimaryButton("Register selected devices",
                        enabled = !busy && peripherals.any { it.selected },
                        onClick = onRegister)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(p: Peripheral, onToggle: (String) -> Unit) {
    val icon = when (p.type) {
        "Watch" -> Icons.Filled.Watch
        "Earbuds" -> Icons.Filled.Headphones
        "Phone" -> Icons.Filled.Smartphone
        else -> Icons.Filled.Bluetooth
    }
    OutlinedCard(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        border = BorderStroke(1.dp,
            if (p.selected) BflCyan else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = BflBlue, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("${p.type} · ${maskMac(p.macAddress)}", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            }
            Checkbox(checked = p.selected, onCheckedChange = { onToggle(p.macAddress) })
        }
    }
}

/** Show only first and last octet; middle hidden for display. */
private fun maskMac(mac: String): String {
    val parts = mac.split(":")
    return if (parts.size == 6) "${parts[0]}:${parts[1]}:••:••:${parts[4]}:${parts[5]}" else mac
}

@Composable
private fun AssistCard(icon: ImageVector, text: String) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = BflGold, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(text, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

/* ---------- 5. Success ---------- */

@Composable
fun SuccessScreen(onHome: () -> Unit) {
    Scaffold(
        topBar = { Header("You're all set") },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                    PrimaryButton("Home", onClick = onHome)
                }
            }
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            Icon(Icons.Filled.CheckCircle, null, tint = androidx.compose.ui.graphics.Color(0xFF1F9D57),
                modifier = Modifier.size(84.dp))
            Spacer(Modifier.height(20.dp))
            Text("Registration complete", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("Your selected devices are registered. When you visit a participating " +
                "outlet, our team can recognise you and share relevant offers.",
                fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
            Spacer(Modifier.height(16.dp))
        }
    }
}

/* ---------- 6. Home (registered landing) ---------- */

@Composable
fun HomeScreen(
    devices: List<Peripheral>,
    onManage: () -> Unit
) {
    Scaffold(topBar = { Header("Home") }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Verified, null,
                            tint = androidx.compose.ui.graphics.Color(0xFF1F9D57))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("You're registered", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("You can be recognised at partner outlets for personalized offers.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("YOUR CONSENTED DEVICES", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = BflNavy, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(10.dp))
                if (devices.isEmpty()) {
                    AssistCard(Icons.Outlined.Info, "Your registered devices will appear here.")
                } else {
                    devices.forEach { p -> RegisteredRow(p) }
                }
                Spacer(Modifier.height(16.dp))
                Text("Changed your mind? You can review or delete your consent anytime.",
                    fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 19.sp,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Surface(shadowElevation = 8.dp) {
                Column(Modifier.padding(16.dp)) {
                    PrimaryButton("Manage my consent", onClick = onManage)
                }
            }
        }
    }
}

@Composable
private fun RegisteredRow(p: Peripheral) {
    val icon = when (p.type) {
        "Watch" -> Icons.Filled.Watch
        "Earbuds" -> Icons.Filled.Headphones
        "Phone" -> Icons.Filled.Smartphone
        else -> Icons.Filled.Bluetooth
    }
    OutlinedCard(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        border = BorderStroke(1.dp, BflCyan)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = BflBlue, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("${p.type} \u00b7 ${maskMacPublic(p.macAddress)}", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            }
            Text("Active", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color(0xFF1F9D57))
        }
    }
}

@Composable
private fun WithdrawPoint(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = BflBlue, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 13.5.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun ManageDeviceRow(p: Peripheral, enabled: Boolean, onRemove: () -> Unit) {
    val icon = when (p.type) {
        "Watch" -> Icons.Filled.Watch
        "Earbuds" -> Icons.Filled.Headphones
        "Phone" -> Icons.Filled.Smartphone
        else -> Icons.Filled.Bluetooth
    }
    OutlinedCard(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = BflBlue, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                Text("${p.type} \u00b7 ${maskMacPublic(p.macAddress)}", fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            }
            TextButton(onClick = onRemove, enabled = enabled) {
                Text("Remove", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

fun maskMacPublic(mac: String): String {
    val parts = mac.split(":")
    return if (parts.size == 6) "${parts[0]}:${parts[1]}:\u2022\u2022:\u2022\u2022:${parts[4]}:${parts[5]}" else mac
}

/* ---------- 7. Manage / withdraw (separate) ---------- */

@Composable
fun ManageScreen(
    registration: RegistrationState,
    devices: List<Peripheral>,
    busy: Boolean,
    onRevokeDevice: (Peripheral) -> Unit,
    onWithdraw: () -> Unit,
    onBack: () -> Unit
) {
    var confirmAll by remember { mutableStateOf(false) }
    var confirmOne by remember { mutableStateOf<Peripheral?>(null) }
    Scaffold(topBar = { Header("Manage consent") }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
                Text("MANAGE CONSENT BY DEVICE", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = BflNavy, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(6.dp))
                Text("Remove consent for any single device, or remove all. Removing a device " +
                    "deletes its identifier from Bajaj Finance servers.",
                    fontSize = 13.5.sp, lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Spacer(Modifier.height(14.dp))
                devices.forEach { p -> ManageDeviceRow(p, enabled = !busy) { confirmOne = p } }
                Spacer(Modifier.height(12.dp))
                Text("Removing your last device withdraws consent entirely.",
                    fontSize = 12.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            }
            Surface(shadowElevation = 8.dp) {
                Column(Modifier.padding(16.dp)) {
                    if (busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Spacer(Modifier.height(12.dp)) }
                    OutlinedButton(
                        onClick = { confirmAll = true },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                    ) { Text("Remove all & withdraw consent", color = MaterialTheme.colorScheme.error) }
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to Home") }
                }
            }
        }
    }

    confirmOne?.let { dev ->
        AlertDialog(
            onDismissRequest = { confirmOne = null },
            title = { Text("Remove this device?") },
            text = { Text("Consent for \"${dev.name}\" will be removed and its identifier deleted " +
                "from our servers. Removing your last device withdraws consent entirely.") },
            confirmButton = {
                TextButton(onClick = { confirmOne = null; onRevokeDevice(dev) }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmOne = null }) { Text("Cancel") } }
        )
    }
    if (confirmAll) {
        AlertDialog(
            onDismissRequest = { confirmAll = false },
            title = { Text("Withdraw consent for all devices?") },
            text = { Text("All your registered devices will be permanently deleted. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmAll = false; onWithdraw() }) {
                    Text("Yes, delete all", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmAll = false }) { Text("Cancel") } }
        )
    }
}
