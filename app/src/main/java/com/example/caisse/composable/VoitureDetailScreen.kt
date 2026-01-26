package com.example.piece.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.piece.R
import com.example.piece.data.MenuViewModel
import com.example.piece.data.Recette
import com.example.piece.data.Voiture
import com.example.piece.util.PdfUtil
import com.example.piece.util.formatPrice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoitureDetailScreen(
    navController: NavController,
    menuViewModel: MenuViewModel
) {
    // 1. Récupérer toutes les données globales
    val recettesList by menuViewModel.allRecettes.collectAsState()
    val voituresList by menuViewModel.voitures.collectAsState()

    // 2. Calculer les statistiques globales
    val totalRecettes = recettesList.filter { it.isRecette }.sumOf { it.amount }
    val totalDepenses = recettesList.filter { !it.isRecette }.sumOf { it.amount }
    val soldeGlobal = totalRecettes - totalDepenses

    // State pour le formulaire d'ajout
    var name by remember { mutableStateOf("Recette diverse") }
    var amountText by remember { mutableStateOf("") }
    var isRecette by remember { mutableStateOf(true) } // true = Recette, false = Dépense

    // Dropdown state
    var expanded by remember { mutableStateOf(false) }
    var selectedVoiture by remember { mutableStateOf<Voiture?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var recetteToDelete by remember { mutableStateOf<Recette?>(null) }
    val devise = menuViewModel.getInfos()?.devise ?: "F"


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestion Parc Auto") }, // Titre générique
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (recettesList.isNotEmpty()) {
                                PdfUtil.generateAndShareWeeklyReport(
                                    context = navController.context,
                                    recettes = recettesList,
                                    voitures = voituresList,
                                    devise = devise
                                )
                            }

                        }
                    )
                    {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = stringResource(id = R.string.settings_description)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            // -------------------- COLONNE GAUCHE (1/3) : STATS & AJOUT --------------------
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // CARTE STATISTIQUES GLOBALES
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Bilan Global", style = MaterialTheme.typography.titleLarge)
                        Divider(Modifier.padding(vertical = 8.dp))

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Recettes:")
                            Text("+ ${formatPrice(totalRecettes, devise)}", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Dépenses:")
                            Text("- ${formatPrice(totalDepenses, devise = devise)}", color = Color(0xFFE91E63), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("SOLDE GLOBAL:", fontWeight = FontWeight.Bold)
                            Text(formatPrice(soldeGlobal,devise), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // CARTE AJOUT OPERATION
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Ajouter une opération", style = MaterialTheme.typography.titleMedium)

                        // Choix Type (Recette / Dépense) - Code simplifié pour l'exemple
                        Row(Modifier.fillMaxWidth()) {
                            FilterChip(
                                selected = isRecette,
                                onClick = { isRecette = true },
                                label = { Text("Recette") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            FilterChip(
                                selected = !isRecette,
                                onClick = { isRecette = false },
                                label = { Text("Dépense") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // LISTE DÉROULANTE VOITURES (Obligatoire)
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedVoiture?.name ?: "Choisir une voiture",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Voiture") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                voituresList.forEach { voiture ->
                                    DropdownMenuItem(
                                        text = { Text(voiture.name) },
                                        onClick = {
                                            selectedVoiture = voiture
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Montant") }, // KeyboardType.Number
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                val amount = amountText.toDoubleOrNull()
                                if (amount != null && selectedVoiture != null) {
                                    menuViewModel.addRecetteForVoiture(
                                        voitureId = selectedVoiture!!.id,
                                        name = name,
                                        amount = amount,
                                        isRecette = isRecette
                                    )
                                    // Reset simple
                                    amountText = ""
                                    name = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedVoiture != null && amountText.isNotEmpty()
                        ) {
                            Text("ENREGISTRER")
                        }
                    }
                }
            }

            // -------------------- COLONNE DROITE (2/3) : HISTORIQUE COMPLET --------------------
            Card(
                modifier = Modifier
                    .weight(2f)
                    .padding(16.dp)
                    .fillMaxHeight()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Historique Complet", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (recettesList.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Aucune opération enregistrée.")
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(recettesList) { recette ->
                                // Astuce: Afficher le nom de la voiture si possible.
                                // Pour cela, il faudrait croiser l'ID avec la liste voituresList ou enrichir l'objet Recette.
                                // Ici, on affiche l'opération standard.
                                RecetteRowBoolean(
                                    r = recette,
                                    onLongPress = {recetteToDelete = recette
                                        showDeleteDialog = true },
                                    devise = devise
                                )
                            }
                        }
                    }
                }
            }
            if (showDeleteDialog && recetteToDelete != null) {
                AlertDialog(
                    onDismissRequest = {
                        showDeleteDialog = false
                        recetteToDelete = null
                    },
                    title = { Text(text = "Supprimer l'opération ?") },
                    text = {
                        Text("Voulez-vous vraiment supprimer l'opération '${recetteToDelete?.name} ?")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                // Action de suppression confirmée
                                recetteToDelete?.let { menuViewModel.deleteRecette(it.id) }
                                showDeleteDialog = false
                                recetteToDelete = null
                            }
                        ) {
                            Text("Supprimer", color = Color.Red)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                recetteToDelete = null
                            }
                        ) {
                            Text("Annuler")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RecetteRowBoolean(r: Recette, devise : String = "F",onLongPress: () -> Unit) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val dateStr = sdf.format(Date(r.date))
    val sign = if (r.isRecette) "+" else "-"

    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .combinedClickable(
                    onClick = { /* option: ouvrir détail/édition plus tard */ },
                    onLongClick = onLongPress
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(r.name, fontWeight = FontWeight.Bold)
                Text(dateStr, style = MaterialTheme.typography.bodySmall)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("$sign ${formatPrice(r.amount,devise)}", fontWeight = FontWeight.Bold)
                Text(if (r.isRecette) "RECETTE" else "DEPENSE", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
