package com.example.caisse.data

import java.util.UUID

private const val PKG = "com.example.caisse"
private fun drawableUri(name: String) =
    "android.resource://$PKG/drawable/$name"

val CATEGORY_BOISSON_ID = UUID.fromString("22222222-2222-2222-1234-222222222222")

val sampleCategories = listOf(
    Category(
        id = CATEGORY_BOISSON_ID,
        name = "Drinks",
        description = "Boissons"
    )
)

val sampleProducts = listOf(

    Produit(
        nom = "Regab",
        prix = 500.00,
        image = drawableUri("regab"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Royal",
        prix = 1000.00,
        image = drawableUri("royal"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Booster",
        prix = 800.00,
        image = drawableUri("boostercola"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Doppel GM",
        prix = 800.00,
        image = drawableUri("doppel"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Beaufort",
        prix = 800.00,
        image = drawableUri("beaufort"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Coca-Cola",
        prix = 500.00,
        image = drawableUri("coca"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 50,
        description = "Boisson gazeuse rafraîchissante"
    ),

    Produit(
        nom = "Orangina",
        prix = 500.00,
        image = drawableUri("orangina"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 100,
        description = "Boisson gazeuse à l’orange"
    ),

    Produit(
        nom = "Top Orange",
        prix = 500.00,
        image = drawableUri("top_orange"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 60,
        description = "Jus d’orange"
    ),

    Produit(
        nom = "Djino",
        prix = 500.00,
        image = drawableUri("djino"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 25,
        description = "Boisson sucrée"
    ),

    Produit(
        nom = "Tonic",
        prix = 280.00,
        image = drawableUri("imperial"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 45,
        description = "Limonade maison"
    ),

    Produit(
        nom = "XXL",
        prix = 900.00,
        image = drawableUri("xxl"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 35,
        description = "Boisson énergisante"
    ),

    Produit(
        nom = "World Cola",
        prix = 500.00,
        image = drawableUri("wordcola"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 30,
        description = "Cola local"
    ),

    Produit(
        nom = "Desperados",
        prix = 1500.00,
        image = drawableUri("despe"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 35,
        description = "Bière aromatisée"
    ),

    Produit(
        nom = "Heineken",
        prix = 1500.00,
        image = drawableUri("heineken"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 35,
        description = "Bière blonde"
    ),

    Produit(
        nom = "Vino",
        prix = 800.00,
        image = drawableUri("vino"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 18,
        description = "Vin blanc"
    ),

    Produit(
        nom = "General Maquis",
        prix = 1500.00,
        image = drawableUri("genef"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 18,
        description = "Vin local"
    ),

    Produit(
        nom = "Bouteille de Vin",
        prix = 3500.00,
        image = drawableUri("genef"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 18,
        description = "Vin de table"
    ),

    Produit(
        nom = "Trecepas",
        prix = 7000.00,
        image = drawableUri("trecepas"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 18,
        description = "Spiritueux premium"
    ),

    Produit(
        nom = "Castel",
        prix = 1500.00,
        image = drawableUri("castel"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 40,
        description = "Bière locale"
    ),

    Produit(
        nom = "Sir Edward's",
        prix = 6000.00,
        image = drawableUri("siredwards"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 18,
        description = "Whisky"
    ),

    Produit(
        nom = "Sumol",
        prix = 1500.00,
        image = drawableUri("sumol"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 25,
        description = "Soda"
    ),

    Produit(
        nom = "Youzou",
        prix = 500.00,
        image = drawableUri("youzou"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 40,
        description = "Boisson gazeuse"
    ),

    Produit(
        nom = "Racine",
        prix = 800.00,
        image = drawableUri("racine"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 20,
        description = "Vin rouge"
    ),

    Produit(
        nom = "Zombie",
        prix = 800.50,
        image = drawableUri("zombie"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 15,
        description = "Cocktail"
    ),

    Produit(
        nom = "Sombrero",
        prix = 500.00,
        image = drawableUri("sombrero"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 28,
        description = "Smoothie"
    ),

    Produit(
        nom = "Guiness",
        prix = 500.00,
        image = drawableUri("guiness"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 22,
        description = "Bière brune"
    ),

    Produit(
        nom = "33 Export",
        prix = 500.00,
        image = drawableUri("export"),
        categoryId = CATEGORY_BOISSON_ID,
        stock = 22,
        description = "Bière blonde"
    )
)


val sampleVendeurs = listOf(
    Vendeur(
        nom = "Ahmed",
        prenom = "dhf"
    )
)