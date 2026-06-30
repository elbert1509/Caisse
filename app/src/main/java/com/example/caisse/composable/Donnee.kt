package com.example.caisse.composable

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.caisse.data.Category
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.Produit
import com.example.caisse.data.Vendeur
import com.example.caisse.data.sampleCategories
import com.example.caisse.data.sampleCategoriesBertrand
import com.example.caisse.data.sampleCategoriesCeny
import com.example.caisse.data.sampleProducts
import com.example.caisse.data.sampleProductsBertrand
import com.example.caisse.data.sampleProductsCeny
import com.example.caisse.data.sampleVendeurs
import com.example.caisse.data.sampleVendeursBertrand
import com.example.caisse.data.sampleVendeursCeny

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Donnee(navController: NavController, menuViewModel: MenuViewModel) {
    var showConfirmClear by remember { mutableStateOf(false) }

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

            // --- Section : jeux de données -------------------------------
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Jeux de données",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Charge un jeu de données pré-rempli (catégories, produits et vendeurs).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DataSetButton("Données Mont CENY") {
                            addSampleData(
                                menuViewModel,
                                sampleCategoriesCeny,
                                sampleProductsCeny,
                                sampleVendeursCeny
                            )
                        }
                        DataSetButton("Données 4G") {
                            addSampleData(
                                menuViewModel,
                                sampleCategoriesBertrand,
                                sampleProductsBertrand,
                                sampleVendeursBertrand
                            )
                        }
                        DataSetButton("jojo") {
                            addSampleData(
                                menuViewModel,
                                sampleCategories,
                                sampleProducts,
                                sampleVendeurs
                            )
                        }
                    }
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

@Composable
private fun RowScope.DataSetButton(label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height(72.dp)
    ) {
        Text(label, textAlign = TextAlign.Center)
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