package com.example.caisse.data


import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.caisse.R
import java.util.UUID


enum class HomeActionButton{
    PRENDRE_COMMANDE,
    HISTORIQUE_COMMANDES,
    PARTAGER_BOUTONS,
    EXPORTER,
    GERER_INVENTAIRE,
    GERER_PRODUITS,
    GERE_CATEGORIE
    }

@Immutable
data class HomeTileData(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val action: HomeActionButton
)

data class Produit (
    val id: String = UUID.randomUUID().toString(),
    val nom : String,
    val prix : Double,
    val image : Int,
    val categorie : String,
)
data class Category(val id: String = UUID.randomUUID().toString(), val name: String)

val sampleCategories = listOf(
    Category(name = "Food"),
    Category(name = "Drinks"),
    Category(name = "Desserts")
)

val sampleProducts = listOf(
    Produit(nom = "Burgeri", prix = 8.00, image = R.drawable.burger, categorie = "Food"),
    Produit(nom = "Pizza", prix = 12.50, image = 0, categorie = "Food"),
    Produit(nom = "Salad", prix = 7.00, image = 0, categorie = "Food"),
    Produit(nom = "Sandwich", prix = 6.50, image = 0, categorie = "Food"),
    Produit(nom = "Pasta", prix = 10.00, image = 0, categorie = "Food"),
    Produit(nom = "Soup", prix = 5.50, image = 0, categorie = "Food"),
    Produit(nom = "Coca-Cola", prix = 2.50, image = 0, categorie = "Drinks"),
    Produit(nom = "Water", prix = 1.50, image = 0, categorie = "Drinks"),
    Produit(nom = "Ice Cream", prix = 4.00, image = 0, categorie = "Desserts"),
    Produit(nom = "Cake", prix = 5.00, image = 0, categorie = "Desserts")
)
