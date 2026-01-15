package com.example.piece.composable

import androidx.compose.foundation.layout.Row
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
import com.example.piece.data.Category
import com.example.piece.data.MenuViewModel
import com.example.piece.data.Produit
import com.example.piece.data.Voiture
import com.example.piece.data.sampleCategories
import com.example.piece.data.sampleProducts
import com.example.piece.data.sampleVendeurs
import com.example.piece.data.sampleVoitures

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Donnee ( menuViewModel: MenuViewModel)
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

            Button(
                modifier = Modifier.padding(padding),
                onClick = { addSampleVoitures(menuViewModel, sampleVoitures) },
                enabled = true,
            ) {
                Text(text = "Add Voitures ")
            }


        }

    }




}



fun addSampleData(menuViewModel: MenuViewModel, sampleCategories: List<Category>, sampleProducts: List<Produit>,) {
    // 1. Ajouter les catégories
    for (category in sampleCategories) {
        menuViewModel.addCategorySample(category)
    }



    // 2. Ajouter les produits
   for (produit in sampleProducts) {
        menuViewModel.addProduit(produit.nom, produit.prix, produit.categoryId,produit.stock, produit.image)
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