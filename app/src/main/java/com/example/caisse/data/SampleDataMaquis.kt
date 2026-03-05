package com.example.caisse.data

import java.util.UUID

private const val PKG = "com.example.caisse"
private fun drawableUri(name: String) =
    "android.resource://$PKG/drawable/$name"

val CATEGORY_POT_ID = UUID.fromString("22222222-2222-2222-1234-222222222222")
val CATEGORY_WHISKY_ID = UUID.fromString("22222222-2222-2222-1235-222222222222")
val CATEGORY_BECTO_ID = UUID.fromString("22222222-2222-2222-4544-222222222222")

val sampleCategoriesMaquis = listOf(
    Category(
        id = CATEGORY_POT_ID,
        name = "Boche",
        description = "Boissons"
    ),
    Category(
        id = CATEGORY_BECTO_ID,
        name = "Becto",
        description = "Boissons"
    ),
    Category(
        id = CATEGORY_WHISKY_ID,
        name = "Whisky",
        description = "Boissons"
    )
)

val sampleProductsMaquis = listOf(

    Produit(
        nom = "Regab",
        prix = 10.00,
        image = drawableUri("regab"),
        categoryId = CATEGORY_POT_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),



    Produit(
        nom = "Booster Zombie",
        prix = 10.00,
        image = drawableUri("zombie"),
        categoryId = CATEGORY_POT_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Coca-Cola",
        prix = 5.00,
        image = drawableUri("coca"),
        categoryId = CATEGORY_POT_ID,
        stock = 50,
        description = "Boisson gazeuse rafraîchissante"
    ),

    Produit(
        nom = "Desperados",
        prix = 5.00,
        image = drawableUri("despe"),
        categoryId = CATEGORY_POT_ID,
        stock = 35,
        description = "Bière aromatisée"
    ),

    Produit(
        nom = "Heineken Formule",
        prix = 10.00,
        image = drawableUri("heineken"),
        categoryId = CATEGORY_POT_ID,
        stock = 35,
        description = "Bière blonde"
    ),

    // Whisky
    Produit(
        nom = "Vodka ",
        prix = 100.00,
        image = drawableUri("vodka"),
        categoryId = CATEGORY_WHISKY_ID,
        stock = 35,
        description = "Bière blonde"
    ),

    Produit(
        nom = "Jack Daniel's ",
        prix = 110.00,
        image = drawableUri("jack"),
        categoryId = CATEGORY_WHISKY_ID,
        stock = 35,
        description = "Bière blonde"
    ),

    Produit(
        nom = "Chivas ",
        prix = 130.00,
        image = drawableUri("chivas"),
        categoryId = CATEGORY_WHISKY_ID,
        stock = 35,
        description = "Bière blonde"
    ),

    Produit(
        nom = "Hennesy",
        prix = 130.00,
        image = drawableUri("henesy"),
        categoryId = CATEGORY_WHISKY_ID,
        stock = 35,
        description = "Bière blonde"
    ),

    Produit(
        nom = "Baileys ",
        prix = 60.00,
        image = drawableUri("baileys"),
        categoryId = CATEGORY_WHISKY_ID,
        stock = 35,
        description = "Bière blonde"
    ),

    Produit(
        nom = "Vin Blanc ",
        prix = 60.00,
        image = drawableUri("vinblanc"),
        categoryId = CATEGORY_WHISKY_ID,
        stock = 35,
        description = "Bière blonde"
    ),

    Produit(
        nom = "Moet",
        prix = 130.00,
        image = drawableUri("moet"),
        categoryId = CATEGORY_WHISKY_ID,
        stock = 35,
        description = "Bière blonde"
    ),

    Produit(
        nom = "Cotis",
        prix = 10.00,
        image = drawableUri("cotis"),
        categoryId = CATEGORY_BECTO_ID,
        stock = 35,
        description = "Bière blonde"
    ),


)


val sampleVendeursMaquis = listOf(
    Vendeur(
        nom = "Ahmed",
        prenom = "dhf"
    )
)