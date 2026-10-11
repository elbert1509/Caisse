package com.example.epicerie.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
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
import com.example.epicerie.data.Category
import com.example.epicerie.data.MenuViewModel
import com.example.epicerie.data.Produit
import com.example.epicerie.data.Voiture
import com.example.epicerie.data.sampleCategories
import com.example.epicerie.data.sampleProducts
import com.example.epicerie.data.sampleVendeurs
import com.example.epicerie.data.sampleVoitures
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Donnee ( menuViewModel: MenuViewModel)
{
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
                title = { Text("Catégories", style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                    }
                }

            )
        }

    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {

            Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                Button(
                    onClick = { addSampleData(menuViewModel, sampleCategories, sampleProducts) },
                    enabled = true,
                ) {
                    Text(text = "Données KA")
                }

                Button(
                    onClick = { addSampleVoitures(menuViewModel, sampleVoitures) },
                    enabled = true,
                ) {
                    Text(text = "Add Voitures ")
                }
            }

            androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))

            // --- Nouveau client (Firebase) ---
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
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
                        Text(if (importing) "Import en cours…" else "Importer ce client")
                    }

                    importStatus?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
    }
}


fun addSampleData(
    menuViewModel: MenuViewModel,
    sampleCategories: List<Category>,
    sampleProducts: List<Produit>,
    sampleVendeurs: List<com.example.epicerie.data.Vendeur> = com.example.epicerie.data.sampleVendeurs
) {
    // 1. Ajouter les catégories
    for (category in sampleCategories) {
        menuViewModel.addCategorySample(category)
    }

    // 2. Ajouter les produits
    for (produit in sampleProducts) {
        menuViewModel.addProduit(
            produit.nom, produit.prix, produit.categoryId, produit.stock,
            produit.image, produit.codeBarre
        )
    }

    // 3. Ajouter vendeur
    for (vendeur in sampleVendeurs) {
        menuViewModel.addVendeur(vendeur)
    }
}

fun addSampleVoitures(menuViewModel: MenuViewModel, sampleVoitures: List<Voiture>){
    for (voiture in sampleVoitures) {
        menuViewModel.addVoiture(voiture.name)
    }

}
