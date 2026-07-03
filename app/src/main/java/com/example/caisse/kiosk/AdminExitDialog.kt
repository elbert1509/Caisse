package com.example.caisse.kiosk

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Boîte de dialogue de maintenance.
 *
 * Branche-la sur un déclencheur DISCRET (ex : appui long 3 s sur le logo, ou
 * 5 taps rapides dans un coin) pour que seul l'admin y accède.
 *
 * Une fois le PIN validé :
 *  - on sort du mode kiosk (LockTask),
 *  - l'admin peut ouvrir les Réglages WiFi / Bluetooth,
 *  - le bouton "Reprendre le kiosk" re-verrouille l'appareil.
 */
@Composable
fun AdminExitDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var unlocked by remember { mutableStateOf(KioskManager.adminMaintenanceMode) }
    var updateStatus by remember { mutableStateOf<String?>(null) }
    var updating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (unlocked) "Maintenance" else "Accès administrateur") },
        text = {
            if (unlocked) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Kiosk désactivé. Vous pouvez configurer l'appareil.")
                    Text("ID de ce pad : ${SilentUpdater.deviceId(context)}")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = { KioskManager.openWifiSettings(context) }) { Text("WiFi") }
                        OutlinedButton(onClick = { KioskManager.openBluetoothSettings(context) }) { Text("Bluetooth") }
                    }
                    OutlinedButton(
                        enabled = !updating,
                        onClick = {
                            updating = true
                            updateStatus = "Vérification…"
                            scope.launch {
                                updateStatus = SilentUpdater.runUpdate(context)
                                updating = false
                            }
                        }
                    ) { Text(if (updating) "Mise à jour…" else "Vérifier les mises à jour") }
                    updateStatus?.let { Text(it) }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it; error = false },
                        label = { Text("Code PIN") },
                        isError = error,
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    if (error) Text("Code incorrect")
                }
            }
        },
        confirmButton = {
            if (unlocked) {
                Button(onClick = {
                    KioskManager.endMaintenance(activity)
                    onDismiss()
                }) { Text("Reprendre le kiosk") }
            } else {
                Button(onClick = {
                    if (KioskManager.tryEnterMaintenance(activity, pin)) {
                        unlocked = true
                    } else {
                        error = true
                    }
                }) { Text("Valider") }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}
