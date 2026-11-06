package com.example.epicerie.kiosk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Mise à jour silencieuse à la demande (écran Gestion, déjà protégé par mot de passe) :
 * même chemin que la vérification automatique — config/app_android_epicerie, ou
 * devices_epicerie/{id} pour ce pad. Sans sortir du kiosk, contrairement à AdminExitDialog.
 */
@Composable
fun MiseAJourDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var status by remember { mutableStateOf<String?>(null) }
    var enCours by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val installResult by SilentUpdater.installResult.collectAsState()

    AlertDialog(
        onDismissRequest = { if (!enCours) onDismiss() },
        title = { Text("Mise à jour de l'application") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Version installée : ${SilentUpdater.currentVersionName(context)}")
                Text("ID de ce pad : ${SilentUpdater.deviceId(context)}")
                status?.let { Text(it) }
                // Résultat réel de l'installation, rapporté plus tard par InstallResultReceiver.
                installResult?.let { Text(it) }
            }
        },
        confirmButton = {
            Button(
                enabled = !enCours,
                onClick = {
                    enCours = true
                    status = "Vérification…"
                    SilentUpdater.clearInstallResult()
                    scope.launch {
                        status = SilentUpdater.runUpdate(context)
                        enCours = false
                    }
                }
            ) { Text(if (enCours) "Mise à jour…" else "Rechercher une mise à jour") }
        },
        dismissButton = {
            OutlinedButton(enabled = !enCours, onClick = onDismiss) { Text("Fermer") }
        }
    )
}
