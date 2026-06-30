package com.example.caisse.data

import java.util.UUID

private const val PKG = "com.example.caisse"
private fun drawableUri(name: String) =
    "android.resource://$PKG/drawable/$name"

val VIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111")
val DRINKS_ID = UUID.fromString("22222222-2222-2222-2222-222222222222")
val  POISSON_ID = UUID.fromString("33333333-3333-3333-3333-333333333333")
val  VIANDE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444")
val  SOUPE_ID = UUID.fromString("44444444-4444-4444-2222-444444444444")
val  SAUCE_ID = UUID.fromString("44444444-4444-4444-3333-444444444444")
val  COMPLEMENT_ID = UUID.fromString("44444444-4444-4444-0000-444444444444")
val  LEGUMES_ID  = UUID.fromString("44444444-4444-4444-1111-444444444444")
val  PLAT_CAMEROUNAIS_ID  = UUID.fromString("44444444-4444-4444-1144-444444444444")
val JUS_ID  = UUID.fromString("44444444-4444-4444-1144-444444444440")
val GOMBO_ID  = UUID.fromString("44444444-4444-4444-1144-444444444441")


val sampleCategoriesBertrand = listOf(
    Category(
        id = VIN_ID,
        name = "Vin",
        description = "Repas et plats principaux"
    ),
    Category(
        id = DRINKS_ID,
        name = "Biere",
        description = "Boissons"
    ),
    Category(
        id =  POISSON_ID,
        name = "Poisson",
        description = "Pâtisseries et douceurs"
    ),
    Category(
        id =  VIANDE_ID,
        name = "Viande",
        description = "Entrées"
    ),
    Category(
        id =  SOUPE_ID,
        name = "Soupe",
        description = "Entrées"
    ),
    Category(
        id =  SAUCE_ID,
        name = "Sauce",
        description = "Entrées"
    ),
    Category(
        id =  COMPLEMENT_ID,
        name = "Accompagnement",
        description = "Entrées"
    ),
    Category(
        id =  LEGUMES_ID,
        name = "Legumes ",
        description = "Plat chaud"
    ),
    Category(
        id =  PLAT_CAMEROUNAIS_ID,
        name = "Plat Camerounais",
        description = "plat chaud"
    ),
    Category(
        id =  JUS_ID,
        name = "Jus",
        description = "Jus"
    ),
    Category(
        id =  GOMBO_ID,
        name = "Gombo",
        description = "Gombo"
    )

)

val sampleProductsBertrand = listOf(

    // Bière
    Produit(
        nom = "Guiness G",
        prix = 10.00,
        image = drawableUri("guiness"),
        categoryId = DRINKS_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Guiness P",
        prix = 6.00,
        image = drawableUri("guiness"),
        categoryId = DRINKS_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Leffe G",
        prix = 7.00,
        image = drawableUri("leffe"),
        categoryId = DRINKS_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),
    Produit(
        nom = "1664 G",
        prix = 5.00,
        image = drawableUri("r1664"),
        categoryId = DRINKS_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),
    Produit(
        nom = "Desperados G",
        prix = 7.00,
        image = drawableUri("despe"),
        categoryId = DRINKS_ID,
        stock = 35,
        description = "Jus de pommes frais"
    ),
    Produit(
        nom = "Desperados P",
        prix = 5.00,
        image = drawableUri("despe"),
        categoryId = DRINKS_ID,
        stock = 35,
        description = "Jus de pommes frais"
    ),





    // Jus

    Produit(
        nom = "Top",
        prix = 7.00,
        image = drawableUri("top"),
        categoryId = JUS_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Coca-Cola",
        prix = 3.50,
        image = drawableUri("coca"),
        categoryId = JUS_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Orangina",
        prix = 3.50,
        image = drawableUri("orangina"),
        categoryId = JUS_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),
    Produit(
        nom = "schweppes",
        prix = 3.50,
        image = drawableUri("schweppes"),
        categoryId = JUS_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),


    // Vin
    Produit(
        nom = "Vin Bordeaux  ",
        prix = 15.00,
        image = drawableUri("vinbordeaux"),
        categoryId = VIN_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),
    Produit(
        nom = "Vin Bordeaux  ",
        prix = 12.00,
        image = drawableUri("vinbordeaux"),
        categoryId = VIN_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    // Grillade Poisson

    Produit(
        nom = "Sole",
        prix = 50.00,
        image = drawableUri("poisson1"),
        categoryId =  POISSON_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),
    Produit(
        nom = "Maquereau",
        prix = 15.00,
        image = drawableUri("poisson1"),
        categoryId =  POISSON_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    Produit(
        nom = "Maquereau",
        prix = 20.00,
        image = drawableUri("poisson1"),
        categoryId =  POISSON_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    Produit(
        nom = "Maquereau",
        prix = 25.00,
        image = drawableUri("poisson1"),
        categoryId =  POISSON_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),
    Produit(
        nom = "Tilapia",
        prix = 15.00,
        image = drawableUri("poisson1"),
        categoryId =  POISSON_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    Produit(
        nom = "Bar Braisé",
        prix = 23.00,
        image = drawableUri("poisson1"),
        categoryId =  POISSON_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),


    // Grillade Viande

    Produit(
        nom = "Aile de Poulet ",
        prix = 10.00,
        image = drawableUri("aile"),
        categoryId =  VIANDE_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),
    Produit(
        nom = "Aile de Poulet ",
        prix = 6.00,
        image = drawableUri("aile"),
        categoryId =  VIANDE_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),
    Produit(
        nom = "Aile de Poulet ",
        prix = 5.00,
        image = drawableUri("aile"),
        categoryId =  VIANDE_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),
    Produit(
        nom = "Brochettes",
        prix = 10.00,
        image = drawableUri("brochette"),
        categoryId =  VIANDE_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    Produit(
        nom = "Porc Braisé",
        prix = 13.00,
        image = drawableUri("porc"),
        categoryId =  VIANDE_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    Produit(
        nom = "Poulet Braisé ",
        prix = 12.00,
        image = drawableUri("poulet"),
        categoryId =  VIANDE_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    // Soupe
    Produit(
        nom = "Bouillon de Poisson",
        prix = 17.00,
        image = drawableUri("soupe"),
        categoryId =  SOUPE_ID,
        stock = 22,
        description = "Soupe de Poisson"
    ),

    Produit(
        nom = "Soupe",
        prix = 13.00,
        image = drawableUri("soupe"),
        categoryId =  SOUPE_ID,
        stock = 22,
        description = "Soupe de Poisson"
    ),


    // Sauce
    Produit(
        nom = "Sauce Tomate ",
        prix = 15.00,
        image = drawableUri("soupe"),
        categoryId =  SAUCE_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    Produit(
        nom = "Maffe",
        prix = 12.00,
        image = drawableUri("maffe"),
        categoryId =  SAUCE_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    Produit(
        nom = "Chieb",
        prix = 12.00,
        image = drawableUri("yassa"),
        categoryId =  SAUCE_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    // Compléments
    Produit(
        nom = " Bâton de Manioc  ",
        prix = 3.00,
        image = drawableUri("manioc"),
        categoryId =  COMPLEMENT_ID,
        stock = 22,
        description = "Complement de Poisson"
    ),

    Produit(
        nom = " Miodo  ",
        prix = 3.00,
        image = drawableUri("miodo"),
        categoryId =  COMPLEMENT_ID,
        stock = 22,
        description = "Complement de Poisson"
    ),

    Produit(
        nom = " Plantain vapeur   ",
        prix = 3.00,
        image = drawableUri("plantainvap"),
        categoryId =  COMPLEMENT_ID,
        stock = 22,
        description = "Complement de Poisson"
    ),

    Produit(
        nom = " Plantain tapé  ",
        prix = 3.00,
        image = drawableUri("tape"),
        categoryId =  COMPLEMENT_ID,
        stock = 22,
        description = "Complement de Poisson"
    ),

    Produit(
        nom = " Riz  ",
        prix = 3.00,
        image = drawableUri("riz"),
        categoryId =  COMPLEMENT_ID,
        stock = 22,
        description = "Complement de Poisson"
    ),

    Produit(
        nom = " Igname",
        prix = 3.00,
        image = drawableUri("igname"),
        categoryId =  COMPLEMENT_ID,
        stock = 22,
        description = "Complement de Poisson"
    ),

    Produit(
        nom = " Aloko",
        prix = 3.00,
        image = drawableUri("alloco"),
        categoryId =  COMPLEMENT_ID,
        stock = 22,
        description = "Complement de Poisson"
    ),


    Produit(
        nom = " Supplément",
        prix = 3.00,
        image = drawableUri("supplement"),
        categoryId =  COMPLEMENT_ID,
        stock = 22,
        description = "Complement de Poisson"
    ),
    // Legumes

    Produit(
        nom = " Ndole Royale",
        prix = 25.00,
        image = drawableUri("ndoleroyale"),
        categoryId =  LEGUMES_ID,
        stock = 22,
        description = "Plat à base de légumes"
    ),
    Produit(
        nom = " Ndole Mixte",
        prix = 22.00,
        image = drawableUri("ndolemixe"),
        categoryId =  LEGUMES_ID,
        stock = 22,
        description = "Plat à base de légumes"
    ),

    Produit(
        nom = "Sauté de Porc",
        prix = 15.00,
        image = drawableUri("sautedeporc"),
        categoryId =  LEGUMES_ID,
        stock = 2,
        description =  "Plat à base de légumes"
    ),

    // Plat camerounais
    Produit(
        nom = "Taro Royal",
        prix = 20.00,
        image = drawableUri("taro"),
        categoryId =  PLAT_CAMEROUNAIS_ID,
        stock = 2,
        description =  "Plats camerounais",
    ),
    Produit(
        nom = "Tripes",
        prix = 20.00,
        image = drawableUri("tripesaute"),
        categoryId =  PLAT_CAMEROUNAIS_ID,
        stock = 2,
        description =  "Plats camerounais",
    ),
    Produit(
        nom = "Hero",
        prix = 20.00,
        image = drawableUri("eru"),
        categoryId =  PLAT_CAMEROUNAIS_ID,
        stock = 2,
        description =  "Plats camerounais",
    ),
    Produit(
        nom = "Hero Royal",
        prix = 25.00,
        image = drawableUri("eru"),
        categoryId =  PLAT_CAMEROUNAIS_ID,
        stock = 2,
        description =  "Plats camerounais",
    ),
    Produit(
        nom = "Poulet DG",
        prix = 20.00,
        image = drawableUri("pouletdg"),
        categoryId =  PLAT_CAMEROUNAIS_ID,
        stock = 3,
        description =  "Plat à base de légumes"
    ),
    Produit(
        nom = "Rôti de tripes",
        prix = 15.00,
        image = drawableUri("tripesaute"),
        categoryId =  LEGUMES_ID,
        stock = 3,
        description =  "Plat à base de légumes"
    ),

    // GOMBO
    Produit(
        nom = "Gombo viande",
        prix = 16.00,
        image = drawableUri("gombo"),
        categoryId =  GOMBO_ID,
        stock = 3,
        description =  "Plat à base de gombo"
    ),
    Produit(
        nom = "Gombo poisson fumé",
        prix = 16.00,
        image = drawableUri("gombo"),
        categoryId =  GOMBO_ID,
        stock = 3,
        description =  "Plat à base de gombo"
    ),
    Produit(
        nom = "Gombo Royale",
        prix = 22.00,
        image = drawableUri("gombo"),
        categoryId =  GOMBO_ID,
        stock = 3,
        description =  "Plat à base de gombo"
    ),
    Produit(
        nom = "Koki",
        prix = 15.00,
        image = drawableUri("koki"),
        categoryId =  GOMBO_ID,
        stock = 3,
        description =  "Plat à base de gombo"
    ),
    Produit(
        nom = "Kondre ",
        prix = 25.00,
        image = drawableUri("kondre"),
        categoryId =  GOMBO_ID,
        stock = 3,
        description =  "Plat à base de gombo"
    ),
    Produit(
        nom = "Macabo râpé",
        prix = 16.00,
        image = drawableUri("gombo"),
        categoryId =  GOMBO_ID,
        stock = 3,
        description =  "Plat à base de gombo"
    ),
    )

val sampleVendeursBertrand = listOf(
    Vendeur(
        nom = "John",
        prenom = "Doe"
    ),
    Vendeur(
        nom = "Jane",
        prenom = "Smith"
    ),
    Vendeur(
        nom = "Bob",
        prenom = "Johnson"
    )
)