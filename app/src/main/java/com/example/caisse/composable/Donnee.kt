package com.example.caisse.composable

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.Produit
import com.example.caisse.data.sampleProducts
import com.example.caisse.data.sampleCategories

@Composable
fun Donnee (navController: NavController, menuViewModel: MenuViewModel)
{
    Button(
        onClick = { addSampleData(menuViewModel) },
        enabled = true,
    ) {
        Text(text = "Données")
    }
}



fun addSampleData(menuViewModel: MenuViewModel) {
    // 1. Ajouter les catégories
    for (category in sampleCategories) {
        menuViewModel.addCategorySample(category)
    }

    // 2. Ajouter les produits
   for (produit in sampleProducts) {
        menuViewModel.addProduit(produit.nom, produit.prix, produit.categoryId)
    }
}