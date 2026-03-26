package com.example.caisse.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.caisse.data.Category
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.Produit
import com.example.caisse.data.Vendeur
import com.example.caisse.data.sampleCategoriesBertrand
import com.example.caisse.data.sampleCategoriesCeny
import com.example.caisse.data.sampleProducts
import com.example.caisse.data.sampleProductsBertrand
import com.example.caisse.data.sampleProductsCeny
import com.example.caisse.data.sampleVendeurs
import com.example.caisse.data.sampleVendeursBertrand
import com.example.caisse.data.sampleVendeursCeny

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Donnee (navController: NavController, menuViewModel: MenuViewModel)
{
    var showConfirmClear by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catégories", style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    IconButton(onClick =
                        {
                            menuViewModel.lockAdmin() // On reverrouille
                            navController.popBackStack()
                        }) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                    }
                }

            )
        }

    ) { padding ->
        Row(
            modifier = Modifier.padding(padding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                modifier = Modifier.padding(padding),
                onClick = { addSampleData(menuViewModel, sampleCategoriesCeny, sampleProductsCeny, sampleVendeursCeny) },
                enabled = true,
            ) {
                Text(text = "Données Mont CENY")
            }

            Button(
                modifier = Modifier.padding(padding),
                onClick = { addSampleData(menuViewModel, sampleCategoriesBertrand, sampleProductsBertrand, sampleVendeursBertrand) },
                enabled = true,
            ) {
                Text(text = "Données 4G")
            }
        }

        Button(
            modifier = Modifier.padding(padding),
            onClick = { showConfirmClear = true }, // On demande confirmation
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            enabled = true,
        ) {
            Text(text = "Clear All Data")
        }

// Boîte de dialogue de sécurité
        if (showConfirmClear) {
            AlertDialog(
                onDismissRequest = { showConfirmClear = false },
                title = { Text("Tout supprimer ?") },
                text = { Text("Voulez-vous vraiment effacer tous les produits, catégories et vendeurs ? Cela ne supprimera pas vos ventes passées.") },
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