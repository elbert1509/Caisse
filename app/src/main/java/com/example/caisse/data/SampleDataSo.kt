package com.example.caisse.data

import java.util.UUID

private const val PKG = "com.example.caisse"
private fun drawableUri(name: String) =
    "android.resource://$PKG/drawable/$name"

val CATEGORY_VINSO_ID = UUID.fromString("bb855498-ef44-4f5f-8a41-a7dcb7d3bccd")
val CATEGORY_BIERESO_ID = UUID.fromString("46300055-77bd-41fa-ac54-fa64aedcb8fc")
val CATEGORY_JUSO_ID = UUID.fromString("c10b9cab-1216-4b4c-9d56-03e30aef2b73")
val CATEGORY_DIVERSO_ID = UUID.fromString("97ee75b5-55d3-4ed2-80c7-cbffeb32df15")

val sampleCategoriesSo = listOf(
    Category(
        id = CATEGORY_VINSO_ID,
        name = "Vin",
        description = "Vins et briques"
    ),
    Category(
        id = CATEGORY_BIERESO_ID,
        name = "Bières",
        description = "Bières et boissons alcoolisées"
    ),
    Category(
        id = CATEGORY_JUSO_ID,
        name = "Jus",
        description = "Jus, sodas et eaux"
    ),
    Category(
        id = CATEGORY_DIVERSO_ID,
        name = "Divers",
        description = "Autres articles"
    )
)

val sampleProductsSo = listOf(

    // ---------- BIÈRES ----------
    Produit(
        nom = "Regab",
        prix = 700.00,
        image = drawableUri("regab"),
        categoryId = CATEGORY_BIERESO_ID,
        stock = 44,
        description = "Bière blonde"
    ),

    Produit(
        nom = "Grande Dopel",
        prix = 800.00,
        image = drawableUri("doppel"),
        categoryId = CATEGORY_BIERESO_ID,
        stock = 24,
        description = "Bière — grand format"
    ),

    Produit(
        nom = "Grande 33",
        prix = 800.00,
        image = drawableUri("export"),
        categoryId = CATEGORY_BIERESO_ID,
        stock = 23,
        description = "Bière blonde — grand format"
    ),

    Produit(
        nom = "Grande Castel",
        prix = 800.00,
        image = drawableUri("castel"),
        categoryId = CATEGORY_BIERESO_ID,
        stock = 8,
        description = "Bière locale — grand format"
    ),

    Produit(
        nom = "Grande Guiness",
        prix = 2000.00,
        image = drawableUri("guiness"),
        categoryId = CATEGORY_BIERESO_ID,
        stock = 8,
        description = "Bière brune — grand format"
    ),

    Produit(
        nom = "Beaufort",
        prix = 800.00,
        image = drawableUri("beaufort"),
        categoryId = CATEGORY_BIERESO_ID,
        stock = 19,
        description = "Bière blonde"
    ),

    Produit(
        nom = "Tequila",
        prix = 800.00,
        image = drawableUri("sombrero"),
        categoryId = CATEGORY_BIERESO_ID,
        stock = 21,
        description = "Tequila (Sombrero)"
    ),

    Produit(
        nom = "Booster",
        prix = 800.00,
        image = drawableUri("boostercola"),
        categoryId = CATEGORY_BIERESO_ID,
        stock = 52,
        description = "Boisson énergisante alcoolisée"
    ),

    Produit(
        nom = "Petite 33",
        prix = 500.00,
        image = drawableUri("export"),
        categoryId = CATEGORY_BIERESO_ID,
        stock = 12,
        description = "Bière blonde — petit format"
    ),

    Produit(
        nom = "Petite Dopel",
        prix = 500.00,
        image = drawableUri("doppel"),
        categoryId = CATEGORY_BIERESO_ID,
        stock = 8,
        description = "Bière — petit format"
    ),

    Produit(
        nom = "Petite Chill",
        prix = 500.00,
        image = drawableUri("chill"),
        categoryId = CATEGORY_BIERESO_ID,
        stock = 17,
        description = "Bière — petit format"
    ),

    Produit(
        nom = "Petite Guiness",
        prix = 1000.00,
        image = drawableUri("guiness"),
        categoryId = CATEGORY_BIERESO_ID,
        stock = 11,
        description = "Bière brune — petit format"
    ),

    Produit(
        nom = "Desperados",
        prix = 2000.00,
        image = drawableUri("despe"),
        categoryId = CATEGORY_BIERESO_ID,
        stock = 10,
        description = "Bière aromatisée"
    ),

    Produit(
        nom = "Moyenne Heineken",
        prix = 1500.00,
        image = drawableUri("heineken"),
        categoryId = CATEGORY_BIERESO_ID,
        stock = 18,
        description = "Bière blonde — format moyen"
    ),

    Produit(
        nom = "1664",
        prix = 1500.00,
        image = drawableUri("r1664"),
        categoryId = CATEGORY_BIERESO_ID,
        stock = 12,
        description = "Bière blonde"
    ),

    Produit(
        nom = "Ice",
        prix = 2000.00,
        image = drawableUri("smirnoff"),
        categoryId = CATEGORY_BIERESO_ID,
        stock = 14,
        description = "Bière aromatisée"
    ),

    Produit(
        nom = "Royal",
        prix = 1500.00,
        image = drawableUri("royal"),
        categoryId = CATEGORY_BIERESO_ID,
        stock = 20,
        description = "Bière blonde"
    ),

    Produit(
        nom = "Bavaria",
        prix = 1300.00,
        image = drawableUri("bavaria"),
        categoryId = CATEGORY_BIERESO_ID,
        stock = 18,
        description = "Bière blonde"
    ),

    Produit(
        nom = "Regab Canette",
        prix = 1000.00,
        image = drawableUri("regab"),
        categoryId = CATEGORY_BIERESO_ID,
        stock = 10,
        description = "Bière blonde en canette"
    ),

    // ---------- VIN ----------
    Produit(
        nom = "Grand Viño",
        prix = 1500.00,
        image = drawableUri("vino"),
        categoryId = CATEGORY_VINSO_ID,
        stock = 2,
        description = "Vin — grand format"
    ),

    Produit(
        nom = "Petit Viño",
        prix = 800.00,
        image = drawableUri("vino"),
        categoryId = CATEGORY_VINSO_ID,
        stock = 11,
        description = "Vin — petit format"
    ),

    Produit(
        nom = "Sovibor",
        prix = 800.00,
        image = drawableUri("sovibor"),
        categoryId = CATEGORY_VINSO_ID,
        stock = 11,
        description = "Petit Mabouela (Sovibor)"
    ),

    Produit(
        nom = "Petit Chenet",
        prix = 2500.00,
        image = drawableUri("chenet"),
        categoryId = CATEGORY_VINSO_ID,
        stock = 7,
        description = "Vin — petit format"
    ),

    Produit(
        nom = "Castillo",
        prix = 2000.00,
        image = drawableUri("castillo"),
        categoryId = CATEGORY_VINSO_ID,
        stock = 2,
        description = "Vin en brique"
    ),

    Produit(
        nom = "Baron",
        prix = 1500.00,
        image = drawableUri("baron"),
        categoryId = CATEGORY_VINSO_ID,
        stock = 2,
        description = "Vin en brique"
    ),

    Produit(
        nom = "Messa",
        prix = 1500.00,
        image = drawableUri("vinbordeaux"),
        categoryId = CATEGORY_VINSO_ID,
        stock = 2,
        description = "Vin en brique"
    ),

    Produit(
        nom = "Tio de la Bota",
        prix = 2000.00,
        image = drawableUri("tio_de_la_bota"),
        categoryId = CATEGORY_VINSO_ID,
        stock = 2,
        description = "Vin en brique"
    ),

    Produit(
        nom = "Capataz",
        prix = 2500.00,
        image = drawableUri("capataz"),
        categoryId = CATEGORY_VINSO_ID,
        stock = 3,
        description = "Vin en brique"
    ),

    Produit(
        nom = "Viñosol",
        prix = 1500.00,
        image = drawableUri("vinosol"),
        categoryId = CATEGORY_VINSO_ID,
        stock = 2,
        description = "Vin en brique"
    ),

    Produit(
        nom = "Grand Chenet",
        prix = 6000.00,
        image = drawableUri("chenet"),
        categoryId = CATEGORY_VINSO_ID,
        stock = 2,
        description = "Vin — grand format"
    ),

    Produit(
        nom = "Grand Versant",
        prix = 4500.00,
        image = drawableUri("grand_versant"),
        categoryId = CATEGORY_VINSO_ID,
        stock = 7,
        description = "Vin de table"
    ),

    Produit(
        nom = "Marquis de Loge",
        prix = 5500.00,
        image = drawableUri("marquis_de_loge"),
        categoryId = CATEGORY_VINSO_ID,
        stock = 2,
        description = "Vin de table"
    ),

    Produit(
        nom = "Souvenir",
        prix = 4500.00,
        image = drawableUri("souvenir"),
        categoryId = CATEGORY_VINSO_ID,
        stock = 2,
        description = "Vin de table"
    ),

    Produit(
        nom = "CH Valac",
        prix = 4500.00,
        image = drawableUri("ch_valac"),
        categoryId = CATEGORY_VINSO_ID,
        stock = 8,
        description = "Vin de table"
    ),

    // ---------- JUS ----------
    Produit(
        nom = "Grand Jus",
        prix = 700.00,
        image = drawableUri("top_orange"),
        categoryId = CATEGORY_JUSO_ID,
        stock = 8,
        description = "Jus — grand format"
    ),

    Produit(
        nom = "Sumol",
        prix = 700.00,
        image = drawableUri("sumol"),
        categoryId = CATEGORY_JUSO_ID,
        stock = 0,
        description = "Soda"
    ),

    Produit(
        nom = "Orangina",
        prix = 800.00,
        image = drawableUri("orangina"),
        categoryId = CATEGORY_JUSO_ID,
        stock = 0,
        description = "Boisson gazeuse à l’orange"
    ),

    Produit(
        nom = "XXL",
        prix = 800.00,
        image = drawableUri("xxl"),
        categoryId = CATEGORY_JUSO_ID,
        stock = 13,
        description = "Boisson énergisante"
    ),

    Produit(
        nom = "Bouteille d'eau (grande)",
        prix = 1000.00,
        image = drawableUri("andza"),
        categoryId = CATEGORY_JUSO_ID,
        stock = 5,
        description = "Eau minérale — grand format"
    ),

    Produit(
        nom = "Bouteille d'eau (petite)",
        prix = 500.00,
        image = drawableUri("andza"),
        categoryId = CATEGORY_JUSO_ID,
        stock = 14,
        description = "Eau minérale — petit format"
    ),

    // ---------- DIVERS ----------
    Produit(
        nom = "Cigarettes",
        prix = 100.00,
        image = drawableUri("cigarette"),
        categoryId = CATEGORY_DIVERSO_ID,
        stock = 34,
        description = "Vente à l’unité"
    )
)

val sampleVendeursSo = listOf(
    Vendeur(
        nom = "Ahmed",
        prenom = "dhf"
    )
)
