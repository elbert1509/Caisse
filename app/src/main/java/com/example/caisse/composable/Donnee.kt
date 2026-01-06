package com.example.caisse.composable

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
import com.example.caisse.data.Category
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.Produit
import com.example.caisse.data.Voiture
import com.example.caisse.data.sampleCategories
import com.example.caisse.data.sampleProducts
import com.example.caisse.data.sampleVendeurs
import com.example.caisse.data.sampleVoitures

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