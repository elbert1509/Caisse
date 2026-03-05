package com.example.caisse.composable

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.sampleCategoriesCeny
import com.example.caisse.data.sampleProducts
import com.example.caisse.data.sampleProductsCeny
import com.example.caisse.data.sampleVendeurs
import com.example.caisse.data.sampleVendeursCeny

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Donnee (navController: NavController, menuViewModel: MenuViewModel)
{

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
        Button(
            modifier = Modifier.padding(padding),
            onClick = { addSampleData(menuViewModel) },
            enabled = true,
        ) {
            Text(text = "Données")
        }
    }




}



fun addSampleData(menuViewModel: MenuViewModel) {
    // 1. Ajouter les catégories
    for (category in sampleCategoriesCeny) {
        menuViewModel.addCategorySample(category)
    }


    // 2. Ajouter les produits
   for (produit in sampleProductsCeny) {
        menuViewModel.addProduit(produit.nom, produit.prix, produit.categoryId,produit.stock, produit.image)
    }

    // 3. Ajouter vendeur
    for (vendeur in sampleVendeursCeny) {
        menuViewModel.addVendeur(vendeur)

    }
}