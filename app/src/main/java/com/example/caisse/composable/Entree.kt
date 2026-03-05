package com.example.caisse.composable

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.caisse.bluetooth.BluetoothViewModel
import com.example.caisse.data.MenuViewModel
import com.example.caisse.model.AuthViewModel
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.caisse.util.formatPrice
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Entree(
    navController: NavController,
    menuViewModel: MenuViewModel,
    bluetoothViewModel: BluetoothViewModel,
    authVm: AuthViewModel
) {
    val ctx = navController.context

    // ✅ Table virtuelle (ID aléatoire)

    val virtualName = "TableVirtuelle"
    var tableUuid by remember { mutableStateOf<UUID?>(null) }

    LaunchedEffect(Unit) {
        // 1) créer (idempotent) puis 2) relire en DB et récupérer l’id
        menuViewModel.addTable(virtualName)

        val t = menuViewModel.getTableByName(virtualName) // 🔽 on ajoute cette méthode
        tableUuid = t?.id
    }

    val safeTableUuid = tableUuid
    if (safeTableUuid == null) {
        // UI de chargement léger
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    // Data (comme TableDetails)
    val products by menuViewModel.produits.collectAsState()
    val tableItems by menuViewModel.tableItems.collectAsState()
    val info = menuViewModel.getInfos()
    val total by menuViewModel.totalAmount.collectAsState()
    val context = LocalContext.current

    var selectedLabel by remember { mutableStateOf<String?>(null) }

    // Charge les items au démarrage
    LaunchedEffect(safeTableUuid) {
        menuViewModel.loadTableItems(safeTableUuid)
    }

    fun addPackByName(productName: String) {
        val p = products.firstOrNull { it.nom.equals(productName, ignoreCase = true) }
        if (p == null) {
            Toast.makeText(
                ctx,
                "Produit introuvable : $productName",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        selectedLabel = productName
        menuViewModel.addProductToTable(p.id, safeTableUuid)
    }

    fun clearSelection() {
        selectedLabel = null

        menuViewModel.clearVirtualTable(safeTableUuid)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Entrées")
                        Text(
                            text = "Choisissez un pack",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            )
        },
        bottomBar = {
            // ✅ Barre d’actions comme TableDetailsScreen
            Surface(tonalElevation = 3.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp, bottom = 60.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Ligne Total
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Text(
                            text = formatPrice(total, info?.devise),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.width(8.dp))

                        IconButton(
                            onClick = { clearSelection() },
                            enabled = total > 0 || selectedLabel != null,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Supprimer",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Boutons Valider / Imprimer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {

                                if (!bluetoothViewModel.isConnected.value) {
                                    Toast.makeText(ctx, "Pas de device connecté", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                menuViewModel.payTable(safeTableUuid)
                                bluetoothViewModel.printEntree(context,tableItems, total, info)
                                navController.popBackStack()
                                authVm.enqueueSync(context = ctx, tag = "sync")
                            },
                            enabled = total > 0
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Valider", maxLines = 1)
                        }

                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (!bluetoothViewModel.isConnected.value) {
                                    Toast.makeText(ctx, "Pas de device connecté", Toast.LENGTH_SHORT).show()
                                    return@OutlinedButton
                                }
                                bluetoothViewModel.printEntree(context,tableItems, total, info)
                                Toast.makeText(ctx, "Ticket imprimé", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            enabled = false
                        ) {
                            Icon(Icons.Filled.Print, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Imprimer", maxLines = 1)
                        }
                    }
                }
            }
        }
    ) { padding ->

        // ✅ écran petit : tout en colonne, spacing propre
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Carte “sélection”
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = selectedLabel ?: "Aucun pack sélectionné",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Appuyez sur un bouton ci-dessous pour choisir.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // 3 boutons uniquement (comme demandé)
            ElevatedButton(
                onClick = { addPackByName("Entrée Solo") },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Text("Entrée Solo", style = MaterialTheme.typography.titleMedium)
            }

            ElevatedButton(
                onClick = { addPackByName("Pack Maquisard") },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Text("Pack Maquisard", style = MaterialTheme.typography.titleMedium)
            }

            ElevatedButton(
                onClick = { addPackByName("Pack VIP Maquisard") },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(Icons.Filled.Star, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Pack VIP Maquis", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(6.dp))
        }
    }
}