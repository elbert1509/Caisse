package com.example.caisse.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.Vendeur

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendeursScreen(navController: NavController, viewModel: MenuViewModel) {
    val vendeurs by viewModel.vendeurs.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var vendeurPourPin by remember { mutableStateOf<Vendeur?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vendeurs") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter un vendeur")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            items(vendeurs) { vendeur ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("${vendeur.prenom} ${vendeur.nom}", fontWeight = FontWeight.Bold)
                            Text(
                                if (vendeur.pinHash.isEmpty()) "PIN non défini" else "PIN défini",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row {
                            IconButton(onClick = { vendeurPourPin = vendeur }) {
                                Icon(Icons.Default.Lock, contentDescription = "Définir le PIN")
                            }
                            IconButton(onClick = { viewModel.deleteVendeur(vendeur) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AjouterVendeurDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { nom, prenom ->
                viewModel.addVendeur(Vendeur(nom = nom, prenom = prenom))
                showAddDialog = false
            }
        )
    }

    vendeurPourPin?.let { vendeur ->
        DefinirPinDialog(
            vendeur = vendeur,
            onDismiss = { vendeurPourPin = null },
            onConfirm = { pin ->
                viewModel.setVendeurPin(vendeur, pin)
                vendeurPourPin = null
            }
        )
    }
}

@Composable
private fun AjouterVendeurDialog(
    onDismiss: () -> Unit,
    onConfirm: (nom: String, prenom: String) -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var prenom by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un vendeur") },
        text = {
            Column {
                OutlinedTextField(value = prenom, onValueChange = { prenom = it }, label = { Text("Prénom") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(enabled = nom.isNotBlank() && prenom.isNotBlank(), onClick = { onConfirm(nom, prenom) }) {
                Text("Ajouter")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
private fun DefinirPinDialog(
    vendeur: Vendeur,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PIN de ${vendeur.prenom} ${vendeur.nom}") },
        text = {
            Column {
                Text("Définissez un code à 4 chiffres pour ce vendeur.")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) { pin = it; error = null } },
                    label = { Text("PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) { confirmation = it; error = null } },
                    label = { Text("Confirmer le PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = pin.length == 4 && confirmation.length == 4,
                onClick = {
                    if (pin != confirmation) {
                        error = "Les deux codes ne correspondent pas."
                    } else {
                        onConfirm(pin)
                    }
                }
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
