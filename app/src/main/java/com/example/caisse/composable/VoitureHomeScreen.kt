package com.example.caisse.composable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.Voiture
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoitureHomeScreen(
    viewModel: MenuViewModel,
    navController: NavController
) {
    val config = LocalConfiguration.current
    val screenWidthDp = config.screenWidthDp

    var selectedTab by remember { mutableIntStateOf(0) }

    // ✅ Doit exister dans MenuViewModel: val voitures: StateFlow<List<Voiture>>
    val voitures by viewModel.voitures.collectAsState()

    // On affiche seulement les voitures non supprimées
    val filteredVoitures = voitures.filter { !it.isDeleted }

    var showDialog by remember { mutableStateOf(false) }
    var voitureName by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var voitureToDelete by remember { mutableStateOf<Voiture?>(null) }

    val columns = when {
        screenWidthDp >= 900 -> 4
        screenWidthDp >= 600 -> 3
        else -> 2
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Caisse PoS") },
                actions = {
                    IconButton(onClick = { /* TODO settings */ }) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                    }
                }
            )
        },
        bottomBar = {
            BottomHome(
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it },
                navController = navController
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Voiture")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                Text(
                    text = "Voitures",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // ✅ Dialog ajout
                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text("Ajouter un véhicule") },
                        text = {
                            TextField(
                                value = voitureName,
                                onValueChange = { voitureName = it },
                                label = { Text("Nom du véhicule") },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (voitureName.isNotBlank()) {
                                        // ✅ Doit exister: fun addVoiture(name: String)
                                        viewModel.addVoiture(voitureName.trim())
                                        voitureName = ""
                                        showDialog = false
                                    }
                                }
                            ) {
                                Text("Ajouter")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showDialog = false }) {
                                Text("Annuler")
                            }
                        }
                    )
                }

                // ✅ Dialog suppression (soft delete recommandé)
                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Supprimer le véhicule ?") },
                        text = { Text("Le véhicule sera désactivé et synchronisé sur vos appareils.") },
                        confirmButton = {
                            TextButton(onClick = {
                                voitureToDelete?.let { v ->
                                    // ✅ Doit exister: fun deleteVoiture(id: UUID)
                                    //viewModel.deleteVoiture(v.id)
                                }
                                voitureToDelete = null
                                showDeleteDialog = false
                            }) { Text("Supprimer") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                voitureToDelete = null
                                showDeleteDialog = false
                            }) { Text("Annuler") }
                        }
                    )
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredVoitures) { voiture ->
                        VoitureListItem(
                            voiture = voiture,
                            onClick = {
                                // route à toi de choisir, exemple:
                                navController.navigate("voiture_details/${voiture.id}")
                            },
                            onLongPress = {
                                voitureToDelete = voiture
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
fun VoitureListItem(
    voiture: Voiture,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(text = voiture.name, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
