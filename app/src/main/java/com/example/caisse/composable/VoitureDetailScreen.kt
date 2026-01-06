package com.example.caisse.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.Recette
import com.example.caisse.util.formatPrice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoitureDetailScreen(
    navController: NavController,
    menuViewModel: MenuViewModel,
    voitureId: String
) {
    val vid = runCatching { UUID.fromString(voitureId) }.getOrNull()
    if (vid == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("ID voiture invalide")
        }
        return
    }

    val voitureName = menuViewModel.getVoitureById(vid)?.name ?: "Nom inconnu"


    var showDeleteDialog by remember { mutableStateOf(false) }
    var recetteToDelete by remember { mutableStateOf<Recette?>(null) }
    // ✅ Flow -> mise à jour dynamique
    val recettesFlow = remember(vid) { menuViewModel.recettesByVoiture(vid) }
    val recettes by recettesFlow.collectAsState()

    var name by remember { mutableStateOf("Recette de la semaine ") }
    var amountText by remember { mutableStateOf("150000") }

    // true = RECETTE, false = DEPENSE
    var isRecette by remember { mutableStateOf(true) }

    val totalRecettes = recettes.filter { it.isRecette }.sumOf { it.amount }
    val totalDepenses = recettes.filter { !it.isRecette }.sumOf { it.amount }
    val solde = totalRecettes - totalDepenses
    val devises = menuViewModel.getInfos()?.devise ?: "F"



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$voitureName  Détails") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // -------------------- GAUCHE: AJOUT --------------------
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Ajouter une opération", style = MaterialTheme.typography.titleLarge)

                    // Toggle type : RECETTE / DEPENSE
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

                        if (isRecette) {
                            // 🔵 RECETTE sélectionnée
                            androidx.compose.material3.Button(
                                onClick = { isRecette = true },
                                modifier = Modifier.weight(1f),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("RECETTE", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { isRecette = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("DEPENSE")
                            }

                        } else {
                            // 🔴 DEPENSE sélectionnée
                            OutlinedButton(
                                onClick = { isRecette = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("RECETTE")
                            }

                            androidx.compose.material3.Button(
                                onClick = { isRecette = false },
                                modifier = Modifier.weight(1f),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("DEPENSE", fontWeight = FontWeight.Bold)
                            }
                        }
                    }


                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nom") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter(Char::isDigit) },
                        label = { Text("Montant (F)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedButton(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank() && amt > 0) {
                                menuViewModel.addRecetteForVoiture(
                                    voitureId = vid,
                                    name = name,
                                    amount = amt,
                                    isRecette = isRecette
                                )

                                // Reset pratique
                                amountText = if (isRecette) "150000" else ""
                                name = if (isRecette) "Recette de la semaine " else "Dépense"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ajouter")
                    }

                    Divider(Modifier.padding(vertical = 6.dp))

                    Text("Résumé", style = MaterialTheme.typography.titleMedium)
                    Text("Total recettes :"+ formatPrice(totalRecettes,devises))
                    Text("Total dépenses : "+ formatPrice(totalDepenses,devises))
                    Text("Solde : "+ formatPrice(solde,devises), fontWeight = FontWeight.Bold)
                }
            }

            // -------------------- DROITE: LISTE --------------------
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Historique", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.padding(6.dp))

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Supprimer l’opération ?") },
                            text = { Text("Cette opération sera supprimée et synchronisée.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    recetteToDelete?.let { menuViewModel.deleteRecette(it.id) }
                                    recetteToDelete = null
                                    showDeleteDialog = false
                                }) { Text("Supprimer") }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    recetteToDelete = null
                                    showDeleteDialog = false
                                }) { Text("Annuler") }
                            }
                        )
                    }

                    if (recettes.isEmpty()) {
                        Text("Aucune opération pour l’instant.")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(recettes) { r ->
                                RecetteRowBoolean(
                                    r,
                                    devises,
                                    onLongPress = {
                                        recetteToDelete = r
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
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
