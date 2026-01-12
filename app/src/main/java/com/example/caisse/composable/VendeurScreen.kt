package com.example.caisse.composable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.Vendeur

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendeurScreen(
    navController: NavController,
    menuViewModel: MenuViewModel
) {
    val vendeurs by menuViewModel.vendeurs.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var prenom by remember { mutableStateOf("") }
    var nom by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var vendeurToDelete by remember { mutableStateOf<Vendeur?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vendeurs") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter vendeur")
            }
        }
    ) { padding ->

        // ---- Dialog ajout ----
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Créer un vendeur") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = prenom,
                            onValueChange = { prenom = it },
                            label = { Text("Prénom") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = nom,
                            onValueChange = { nom = it },
                            label = { Text("Nom") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val p = prenom.trim()
                        val n = nom.trim()
                        if (p.isNotEmpty() && n.isNotEmpty()) {
                            menuViewModel.addVendeur(Vendeur(nom = n, prenom = p))
                            prenom = ""
                            nom = ""
                            showAddDialog = false
                        }
                    }) { Text("Créer") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Annuler") }
                }
            )
        }

        // ---- Dialog suppression ----
        if (showDeleteDialog && vendeurToDelete != null) {
            val v = vendeurToDelete!!
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    vendeurToDelete = null
                },
                title = { Text("Supprimer le vendeur ?") },
                text = { Text("Supprimer ${v.prenom} ${v.nom} ?") },
                confirmButton = {
                    TextButton(onClick = {
                        menuViewModel.deleteVendeur(v)
                        showDeleteDialog = false
                        vendeurToDelete = null
                    }) { Text("Supprimer") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        vendeurToDelete = null
                    }) { Text("Annuler") }
                }
            )
        }

        // ---- Liste ----
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(12.dp)
                .fillMaxSize()
        ) {
            Text(
                "Liste des vendeurs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))

            if (vendeurs.isEmpty()) {
                Text("Aucun vendeur.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(vendeurs, key = { it.id }) { vendeur ->
                        VendeurRow(
                            vendeur = vendeur,
                            onDelete = {
                                vendeurToDelete = vendeur
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VendeurRow(
    vendeur: Vendeur,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { /* option : éditer */ },
                onLongClick = { onDelete() }
            ),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("${vendeur.prenom} ${vendeur.nom}", fontWeight = FontWeight.SemiBold)
                Text("ID: ${vendeur.id}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
            }
        }
    }
}
