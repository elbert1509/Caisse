package com.example.caisse.composable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.Vente
import com.example.caisse.util.formatPrice
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenteScreen(
    navController: NavController,
    viewModel: MenuViewModel
) {
    val ventes by viewModel.ventes.collectAsState()   // Flow<List<Vente>>
    var toDelete by remember { mutableStateOf<Vente?>(null) }

    // On n’affiche que les ventes non supprimées (soft delete)
    val visibleVentes = remember(ventes) { ventes.filter { !it.isDeleted } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ventes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceBright)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            if (visibleVentes.isEmpty()) {
                Text("Aucune vente.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(visibleVentes, key = { it.id }) { v ->
                        VenteRow(
                            vente = v,
                            onDelete = { toDelete = v },
                            devise = viewModel.getInfos()?.devise ?: ""
                        )
                    }
                }
            }
        }

        if (toDelete != null) {
            ConfirmDeleteDialog(
                onDismiss = { toDelete = null },
                onConfirm = {
                    toDelete?.let { viewModel.deleteVenteWithStock(it) }
                    toDelete = null
                }
            )
        }
    }
}

@Composable
private fun VenteRow(vente: Vente, devise: String, onDelete: () -> Unit) {
    val df = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Total :" + formatPrice(vente.total,devise), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("Date : ${df.format(Date(vente.date))}", color = MaterialTheme.colorScheme.onSurfaceVariant)

            val isTable = vente.tableId != null && vente.tableId != UUID.fromString("22222222-0000-2222-2222-222222222222")
            val label = if (isTable) "Type : Table" else "Type : Panier"
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Supprimer")
                }
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Supprimer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
        title = { Text("Supprimer la vente ?") },
        text = { Text("La vente sera marquée supprimée et le stock des produits sera rétabli.") }
    )
}
