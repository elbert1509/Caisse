package com.example.caisse.composable

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.caisse.data.Category
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.Produit
import com.example.caisse.data.Vendeur
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Donnee(navController: NavController, menuViewModel: MenuViewModel) {
    var showConfirmClear by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // --- Import de catalogue client depuis Firebase ---
    var clients by remember { mutableStateOf<List<MenuViewModel.CatalogueClient>>(emptyList()) }
    var selectedClient by remember { mutableStateOf<MenuViewModel.CatalogueClient?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var loadingClients by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var importStatus by remember { mutableStateOf<String?>(null) }

    fun refreshClients() {
        loadingClients = true
        scope.launch {
            runCatching { menuViewModel.listCataloguesClients() }
                .onSuccess { clients = it }
                .onFailure { importStatus = "Échec du chargement de la liste : ${it.message}" }
            loadingClients = false
        }
    }

    LaunchedEffect(Unit) { refreshClients() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Catégories", style = MaterialTheme.typography.headlineSmall)
                },
                actions = {
                    IconButton(onClick = {
                        menuViewModel.lockAdmin() // On reverrouille
                        navController.popBackStack()
                    }) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = "Verrouiller et quitter"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // --- Section : import d'un catalogue client (Firebase) --------
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Nouveau client (Firebase)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(
                            onClick = { refreshClients() },
                            enabled = !loadingClients
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Rafraîchir la liste")
                        }
                    }
                    Text(
                        "Importe le catalogue (catégories, produits, vendeurs) d'un client " +
                                "déclaré dans Firestore — aucune mise à jour de l'app requise.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            readOnly = true,
                            value = selectedClient?.label
                                ?: if (loadingClients) "Chargement…" else "Sélectionner un client",
                            onValueChange = {},
                            label = { Text("Client") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            if (clients.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(if (loadingClients) "Chargement…" else "Aucun client trouvé") },
                                    onClick = { dropdownExpanded = false },
                                    enabled = false
                                )
                            }
                            clients.forEach { client ->
                                DropdownMenuItem(
                                    text = { Text(client.label) },
                                    onClick = {
                                        selectedClient = client
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Button(
                        enabled = selectedClient != null && !importing,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val client = selectedClient ?: return@Button
                            importing = true
                            importStatus = "Import en cours…"
                            scope.launch {
                                runCatching { menuViewModel.fetchCatalogue(client.id) }
                                    .onSuccess { result ->
                                        addSampleData(menuViewModel, result.categories, result.produits, result.vendeurs)
                                        importStatus = "Catalogue « ${client.label} » importé " +
                                                "(${result.categories.size} catégories, " +
                                                "${result.produits.size} produits, " +
                                                "${result.vendeurs.size} vendeurs)."
                                    }
                                    .onFailure { importStatus = "Échec de l'import : ${it.message}" }
                                importing = false
                            }
                        }
                    ) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (importing) "Import en cours…" else "Importer ce client")
                    }

                    importStatus?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
            }

            // --- Section : zone de danger --------------------------------
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Zone de danger",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Cette action efface tous les produits, catégories et vendeurs. " +
                                "Les ventes passées sont conservées.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { showConfirmClear = true }, // On demande confirmation
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Effacer toutes les données")
                    }
                }
            }
        }
    }

    // Boîte de dialogue de sécurité
    if (showConfirmClear) {
        AlertDialog(
            onDismissRequest = { showConfirmClear = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
            title = { Text("Tout supprimer ?") },
            text = {
                Text(
                    "Voulez-vous vraiment effacer tous les produits, catégories et vendeurs ? " +
                            "Cela ne supprimera pas vos ventes passées."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    menuViewModel.clearAllData()
                    showConfirmClear = false
                }) {
                    Text("Confirmer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClear = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

fun addSampleData(menuViewModel: MenuViewModel, category  : List<Category>, produit : List<Produit>, vendeur : List<Vendeur>) {
    // 1. Ajouter les catégories
    for (category in category) {
        menuViewModel.addCategorySample(category)
    }


    // 2. Ajouter les produits
    for (produit in produit) {
        menuViewModel.addProduit(produit.nom, produit.prix, produit.categoryId,produit.stock, produit.image)
    }

    // 3. Ajouter vendeur
    for (vendeur in vendeur) {
        menuViewModel.addVendeur(vendeur)

    }
}

fun clearSampleData(menuViewModel: MenuViewModel){

}