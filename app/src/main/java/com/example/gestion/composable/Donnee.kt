package com.example.gestion.composable

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gestion.data.Category
import com.example.gestion.data.MenuViewModel
import com.example.gestion.data.Produit
import com.example.gestion.data.sampleCategories
import com.example.gestion.data.sampleProducts
import com.example.gestion.data.sampleVendeurs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Donnee (navController: NavController, menuViewModel: MenuViewModel)
{

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
        Row(
            modifier = Modifier.padding(padding),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,

        ) {
            Button(
                modifier = Modifier.padding(padding),
                onClick = { addSampleData(menuViewModel, sampleCategories, sampleProducts) },
                enabled = true,
            ) {
                Text(text = "Données KA")
            }

        }

    }




}



fun addSampleData(menuViewModel: MenuViewModel, sampleCategories: List<Category>, sampleProducts: List<Produit>, ) {
    // 1. Ajouter les catégories
    for (category in sampleCategories) {
        menuViewModel.addCategorySample(category)
    }


    // 2. Ajouter les produits
   for (produit in sampleProducts) {
        menuViewModel.addProduit(produit.nom, produit.prix, produit.categoryId,produit.stock, produit.image, produit.prix_achat)
    }

    // 3. Ajouter vendeur
    for (vendeur in sampleVendeurs) {
        menuViewModel.addVendeur(vendeur)

    }
}