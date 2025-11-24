package com.example.caisse.data

import com.example.caisse.R
import java.util.UUID



val CATEGORY_DRINKS_ID = UUID.fromString("22222222-2222-2222-2222-222222222222")


val sampleCategories = listOf(
    Category(id = CATEGORY_DRINKS_ID, name = "Drinks", description = "Boissons"),

)
val sampleProducts = listOf(

    // --- Drinks (15 produits) ---
    Produit(nom = "Regab", prix = 500.00, image = R.drawable.regab, categoryId = CATEGORY_DRINKS_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "Royal", prix = 1000.00, image = R.drawable.royal, categoryId = CATEGORY_DRINKS_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "Booster", prix = 800.00, image = R.drawable.regab, categoryId = CATEGORY_DRINKS_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "Doppel GM", prix = 800.00, image = R.drawable.doppel, categoryId = CATEGORY_DRINKS_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "Beaufort", prix = 800.0, image = R.drawable.beaufort, categoryId = CATEGORY_DRINKS_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "Coca-Cola", prix = 500.0, image = R.drawable.coca, categoryId = CATEGORY_DRINKS_ID, stock = 50, description = "Boisson gazeuse rafraîchissante"),
    Produit(nom = "Orangina", prix = 500.0, image = R.drawable.orangina, categoryId = CATEGORY_DRINKS_ID, stock = 100, description = "Eau minérale en bouteille"),
    Produit(nom = "Top Orange", prix =  500.0, image = R.drawable.top_orange, categoryId = CATEGORY_DRINKS_ID, stock = 60, description = "Petit café serré"),
    Produit(nom = "Djino", prix =  500.0, image = R.drawable.djino, categoryId = CATEGORY_DRINKS_ID, stock = 25, description = "Café au lait doux"),
    Produit(nom = "Tonic", prix = 2.80, image = R.drawable.imperial, categoryId = CATEGORY_DRINKS_ID, stock = 45, description = "Limonade maison"),
    Produit(nom = "XXL", prix = 900.00, image = R.drawable.xxl, categoryId = CATEGORY_DRINKS_ID, stock = 35, description = "Jus de pommes frais"),
    Produit(nom = "World Cola", prix = 500.0, image = R.drawable.wordcola, categoryId = CATEGORY_DRINKS_ID, stock = 30, description = "Thé glacé au citron"),
    Produit(nom = "Desperados", prix = 1500.00, image = R.drawable.despe, categoryId = CATEGORY_DRINKS_ID, stock = 35, description = "Jus de pommes frais"),
    Produit(nom = "Heineken", prix = 1500.00, image = R.drawable.heineken, categoryId = CATEGORY_DRINKS_ID, stock = 35, description = "Jus de pommes frais"),
    Produit(nom = "Vino ", prix = 800.0, image = R.drawable.vino, categoryId = CATEGORY_DRINKS_ID, stock = 18, description = "Vin blanc de Bourgogne"),
    Produit(nom = "General Maquis ", prix = 1500.00, image = R.drawable.genef, categoryId = CATEGORY_DRINKS_ID, stock = 18, description = "Vin blanc de Bourgogne"),
    Produit(nom = "Bouteille de Vin ", prix = 3500.00, image = R.drawable.genef, categoryId = CATEGORY_DRINKS_ID, stock = 18, description = "Vin blanc de Bourgogne"),
    Produit(nom = "Trecepas", prix = 7000.00, image = R.drawable.trecepas, categoryId = CATEGORY_DRINKS_ID, stock = 18, description = "Vin blanc de Bourgogne"),
    Produit(nom = "Castel", prix = 1500.00, image = R.drawable.castel, categoryId = CATEGORY_DRINKS_ID, stock = 40, description = "Jus d’orange pressé"),
    Produit(nom = "Sir Edward's", prix = 6000.00, image = R.drawable.siredwards, categoryId = CATEGORY_DRINKS_ID, stock = 18, description = "Vin blanc de Bourgogne"),
    Produit(nom = "Sumol ", prix = 1500.0, image = R.drawable.sumol, categoryId = CATEGORY_DRINKS_ID, stock = 25, description = "Café mousseux italien"),
    Produit(nom = "Youzou", prix = 500.00, image = R.drawable.youzou, categoryId = CATEGORY_DRINKS_ID, stock = 40, description = "Bière blonde pression"),
    Produit(nom = "Racine", prix = 800.00, image = R.drawable.racine, categoryId = CATEGORY_DRINKS_ID, stock = 20, description = "Vin rouge de Bordeaux"),
    Produit(nom = "Zombie", prix = 800.50, image = R.drawable.zombie, categoryId = CATEGORY_DRINKS_ID, stock = 15, description = "Cocktail au rhum et menthe"),
    Produit(nom = "Sombrero", prix = 500.0, image = R.drawable.sombrero, categoryId = CATEGORY_DRINKS_ID, stock = 28, description = "Smoothie aux fruits frais"),
    Produit(nom = "Guiness", prix = 500.00, image = R.drawable.guiness, categoryId = CATEGORY_DRINKS_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "Castel", prix = 500.00, image = R.drawable.castel, categoryId = CATEGORY_DRINKS_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "33 Export", prix = 500.00, image = R.drawable.export, categoryId = CATEGORY_DRINKS_ID, stock = 22, description = "Milkshake à la vanille"),



)

val sampleVendeurs = listOf(
    Vendeur(id = 1, nom = "John", prenom = "Doe"),
    Vendeur(id = 2, nom = "Jane", prenom = "Smith"),
    Vendeur(id = 3, nom = "Bob", prenom = "Johnson")
)