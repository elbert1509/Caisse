package com.example.caisse.data

import java.util.UUID

private const val PKG = "com.example.caisse"
private fun drawableUri(name: String) =
    "android.resource://$PKG/drawable/$name"

val CATEGORY_VIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111")
val CATEGORY_DRINKS_ID = UUID.fromString("22222222-2222-2222-2222-222222222222")
val CATEGORY_POISSON_ID = UUID.fromString("33333333-3333-3333-3333-333333333333")
val CATEGORY_VIANDE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444")
val CATEGORY_SOUPE_ID = UUID.fromString("44444444-4444-4444-2222-444444444444")
val CATEGORY_SAUCE_ID = UUID.fromString("44444444-4444-4444-3333-444444444444")
val CATEGORY_COMPLEMENT_ID = UUID.fromString("44444444-4444-4444-0000-444444444444")
val CATEGORY_LEGUMES_ID  = UUID.fromString("44444444-4444-4444-1111-444444444444")
val CATEGORY_PLAT_CAMEROUNAIS_ID  = UUID.fromString("44444444-4444-4444-1144-444444444444")


val sampleCategoriesCeny = listOf(
    Category(
        id = CATEGORY_VIN_ID,
        name = "Vin",
        description = "Repas et plats principaux"
    ),
    Category(
        id = CATEGORY_DRINKS_ID,
        name = "Biere",
        description = "Boissons"
    ),
    Category(
        id = CATEGORY_POISSON_ID,
        name = "Poisson",
        description = "Pâtisseries et douceurs"
    ),
    Category(
        id = CATEGORY_VIANDE_ID,
        name = "Viande",
        description = "Entrées"
    ),
    Category(
        id = CATEGORY_SOUPE_ID,
        name = "Soupe",
        description = "Entrées"
    ),
    Category(
        id = CATEGORY_SAUCE_ID,
        name = "Sauce",
        description = "Entrées"
    ),
    Category(
        id = CATEGORY_COMPLEMENT_ID,
        name = "Accompagnement",
        description = "Entrées"
    ),
    Category(
        id = CATEGORY_LEGUMES_ID,
        name = "Legumes ",
        description = "Plat chaud"
    ),
    Category(
        id = CATEGORY_PLAT_CAMEROUNAIS_ID,
        name = "Plat Camerounais",
        description = "plat chaud"
    ),

)

val sampleProductsCeny = listOf(

    // Bière
    Produit(
        nom = "Guiness G",
        prix = 10.00,
        image = drawableUri("guiness"),
        categoryId = CATEGORY_DRINKS_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Guiness P",
        prix = 5.00,
        image = drawableUri("guiness"),
        categoryId = CATEGORY_DRINKS_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Leffe G",
        prix = 8.00,
        image = drawableUri("leffe"),
        categoryId = CATEGORY_DRINKS_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Leffe P ",
        prix = 4.00,
        image = drawableUri("leffe"),
        categoryId = CATEGORY_DRINKS_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Leffe Formule",
        prix = 12.00,
        image = drawableUri("leffeform"),
        categoryId = CATEGORY_DRINKS_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "1664 G",
        prix = 10.00,
        image = drawableUri("r1664"),
        categoryId = CATEGORY_DRINKS_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "1664 P",
        prix = 10.00,
        image = drawableUri("r1664"),
        categoryId = CATEGORY_DRINKS_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "1664 Formule",
        prix = 10.00,
        image = drawableUri("r16form"),
        categoryId = CATEGORY_DRINKS_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Desperados G",
        prix = 8.00,
        image = drawableUri("despe"),
        categoryId = CATEGORY_DRINKS_ID,
        stock = 35,
        description = "Jus de pommes frais"
    ),

    Produit(
        nom = "Desperados P",
        prix = 5.00,
        image = drawableUri("despe"),
        categoryId = CATEGORY_DRINKS_ID,
        stock = 35,
        description = "Jus de pommes frais"
    ),

    Produit(
        nom = "Desperados Formule",
        prix = 12.00,
        image = drawableUri("despeform"),
        categoryId = CATEGORY_DRINKS_ID,
        stock = 35,
        description = "Jus de pommes frais"
    ),

    Produit(
        nom = "Castel",
        prix = 8.00,
        image = drawableUri("castel"),
        categoryId = CATEGORY_DRINKS_ID,
        stock = 35,
        description = "Jus de pommes frais"
    ),

    Produit(
        nom = "33 Export",
        prix = 8.00,
        image = drawableUri("export"),
        categoryId = CATEGORY_DRINKS_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Kandji",
        prix = 8.00,
        image = drawableUri("export"),
        categoryId = CATEGORY_DRINKS_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Whisky kola",
        prix = 8.00,
        image = drawableUri("boostercola"),
        categoryId = CATEGORY_DRINKS_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Top",
        prix = 6.00,
        image = drawableUri("top"),
        categoryId = CATEGORY_DRINKS_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Djino",
        prix = 6.00,
        image = drawableUri("djino"),
        categoryId = CATEGORY_DRINKS_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    // Vin
    Produit(
        nom = "Moelleux ",
        prix = 20.00,
        image = drawableUri("vinblanc"),
        categoryId = CATEGORY_VIN_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Cabernet d'Anjou ",
        prix = 15.00,
        image = drawableUri("cabernet"),
        categoryId = CATEGORY_VIN_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Côte de Rhone ",
        prix = 20.00,
        image = drawableUri("cabernet"),
        categoryId = CATEGORY_VIN_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Côte de Provence ",
        prix = 20.00,
        image = drawableUri("cabernet"),
        categoryId = CATEGORY_VIN_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Mouton Cadet ",
        prix = 20.00,
        image = drawableUri("moutoncadet"),
        categoryId = CATEGORY_VIN_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Saint Emilion ",
        prix = 30.00,
        image = drawableUri("saintemilion"),
        categoryId = CATEGORY_VIN_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Vin Bordeaux ",
        prix = 20.00,
        image = drawableUri("vinbordeaux"),
        categoryId = CATEGORY_VIN_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Vin Bordeaux P ",
        prix = 10.00,
        image = drawableUri("vinbordeaux"),
        categoryId = CATEGORY_VIN_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    Produit(
        nom = "Vin Bordeaux P ",
        prix = 15.00,
        image = drawableUri("vinbordeaux"),
        categoryId = CATEGORY_VIN_ID,
        stock = 22,
        description = "Milkshake à la vanille"
    ),

    // Grillade Poisson
    Produit(
        nom = "Malangwa",
        prix = 20.00,
        image = drawableUri("poisson1"),
        categoryId = CATEGORY_POISSON_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    Produit(
        nom = "Sole",
        prix = 40.00,
        image = drawableUri("poisson1"),
        categoryId = CATEGORY_POISSON_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    Produit(
        nom = "Capitaine ",
        prix = 20.00,
        image = drawableUri("poisson1"),
        categoryId = CATEGORY_POISSON_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    Produit(
        nom = "Capitaine ",
        prix = 25.00,
        image = drawableUri("poisson1"),
        categoryId = CATEGORY_POISSON_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    Produit(
        nom = "Capitaine ",
        prix = 30.00,
        image = drawableUri("poisson1"),
        categoryId = CATEGORY_POISSON_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    Produit(
        nom = "Maquereau",
        prix = 15.00,
        image = drawableUri("poisson1"),
        categoryId = CATEGORY_POISSON_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    Produit(
        nom = "Maquereau",
        prix = 20.00,
        image = drawableUri("poisson1"),
        categoryId = CATEGORY_POISSON_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    Produit(
        nom = "Maquereau",
        prix = 25.00,
        image = drawableUri("poisson1"),
        categoryId = CATEGORY_POISSON_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    Produit(
        nom = "Tilapia",
        prix = 15.00,
        image = drawableUri("poisson1"),
        categoryId = CATEGORY_POISSON_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    Produit(
        nom = "Tilapia",
        prix = 20.00,
        image = drawableUri("poisson1"),
        categoryId = CATEGORY_POISSON_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    // Grillade Viande
    Produit(
        nom = "Brochettes",
        prix = 10.00,
        image = drawableUri("brochette"),
        categoryId = CATEGORY_VIANDE_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    Produit(
        nom = "Porc Braisé",
        prix = 10.00,
        image = drawableUri("porc"),
        categoryId = CATEGORY_VIANDE_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    Produit(
        nom = "Gesier ",
        prix = 10.00,
        image = drawableUri("porc"),
        categoryId = CATEGORY_VIANDE_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    Produit(
        nom = "Poulet Braisé ",
        prix = 10.00,
        image = drawableUri("poulet"),
        categoryId = CATEGORY_VIANDE_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    Produit(
        nom = "Aile de Poulet ",
        prix = 10.00,
        image = drawableUri("aile"),
        categoryId = CATEGORY_VIANDE_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    // Soupe
    Produit(
        nom = "Bouillon de Poisson",
        prix = 15.00,
        image = drawableUri("soupe"),
        categoryId = CATEGORY_SOUPE_ID,
        stock = 22,
        description = "Soupe de Poisson"
    ),

    Produit(
        nom = "Bouillon de Poisson",
        prix = 20.00,
        image = drawableUri("soupe"),
        categoryId = CATEGORY_SOUPE_ID,
        stock = 22,
        description = "Soupe de Poisson"
    ),

    Produit(
        nom = "Bouillon de Poisson",
        prix = 25.00,
        image = drawableUri("soupe"),
        categoryId = CATEGORY_SOUPE_ID,
        stock = 22,
        description = "Soupe de Poisson"
    ),

    Produit(
        nom = "Bouillon de Boeuf ",
        prix = 15.00,
        image = drawableUri("soupe"),
        categoryId = CATEGORY_SOUPE_ID,
        stock = 22,
        description = "Soupe de Poisson"
    ),

    Produit(
        nom = "Bouillon de Porc",
        prix = 15.00,
        image = drawableUri("soupe"),
        categoryId = CATEGORY_SOUPE_ID,
        stock = 22,
        description = "Soupe de Poisson"
    ),

    Produit(
        nom = "Rôti de Porc",
        prix = 15.00,
        image = drawableUri("soupe"),
        categoryId = CATEGORY_SOUPE_ID,
        stock = 22,
        description = "Soupe de Poisson"
    ),

    // Sauce
    Produit(
        nom = "Sauce Tomate ",
        prix = 15.00,
        image = drawableUri("soupe"),
        categoryId = CATEGORY_SAUCE_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    Produit(
        nom = "Maffe Viande ",
        prix = 15.00,
        image = drawableUri("maffe"),
        categoryId = CATEGORY_SAUCE_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    Produit(
        nom = "Maffe Poisson fumé ",
        prix = 15.00,
        image = drawableUri("maffe"),
        categoryId = CATEGORY_SAUCE_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    Produit(
        nom = "Gombo (Viande + Poisson) ",
        prix = 18.00,
        image = drawableUri("gombo"),
        categoryId = CATEGORY_SAUCE_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    Produit(
        nom = "Gombo Poisson fumé ",
        prix = 15.00,
        image = drawableUri("gombo"),
        categoryId = CATEGORY_SAUCE_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    Produit(
        nom = "Yassa Poulet ",
        prix = 15.00,
        image = drawableUri("yassa"),
        categoryId = CATEGORY_SAUCE_ID,
        stock = 22,
        description = "Grillade de Poisson"
    ),

    // Compléments
    Produit(
        nom = " Bâton de Manioc  ",
        prix = 0.00,
        image = drawableUri("manioc"),
        categoryId = CATEGORY_COMPLEMENT_ID,
        stock = 22,
        description = "Complement de Poisson"
    ),

    Produit(
        nom = " Miodo  ",
        prix = 0.00,
        image = drawableUri("miodo"),
        categoryId = CATEGORY_COMPLEMENT_ID,
        stock = 22,
        description = "Complement de Poisson"
    ),

    Produit(
        nom = " Plantain vapeur   ",
        prix = 0.00,
        image = drawableUri("plantainvap"),
        categoryId = CATEGORY_COMPLEMENT_ID,
        stock = 22,
        description = "Complement de Poisson"
    ),

    Produit(
        nom = " Plantain tapé  ",
        prix = 0.00,
        image = drawableUri("tape"),
        categoryId = CATEGORY_COMPLEMENT_ID,
        stock = 22,
        description = "Complement de Poisson"
    ),

    Produit(
        nom = " Riz  ",
        prix = 0.00,
        image = drawableUri("riz"),
        categoryId = CATEGORY_COMPLEMENT_ID,
        stock = 22,
        description = "Complement de Poisson"
    ),

    Produit(
        nom = " Semoule",
        prix = 0.00,
        image = drawableUri("semoule"),
        categoryId = CATEGORY_COMPLEMENT_ID,
        stock = 22,
        description = "Complement de Poisson"
    ),

    Produit(
        nom = " Aloko",
        prix = 0.00,
        image = drawableUri("alloco"),
        categoryId = CATEGORY_COMPLEMENT_ID,
        stock = 22,
        description = "Complement de Poisson"
    ),

    Produit(
        nom = " Attieké",
        prix = 0.00,
        image = drawableUri("attikie"),
        categoryId = CATEGORY_COMPLEMENT_ID,
        stock = 22,
        description = "Complement de Poisson"
    ),

    Produit(
        nom = " Supplément",
        prix = 5.00,
        image = drawableUri("supplement"),
        categoryId = CATEGORY_COMPLEMENT_ID,
        stock = 22,
        description = "Complement de Poisson"
    ),
    // Legumes

    Produit(
        nom = " Ndole Royale",
        prix = 20.00,
        image = drawableUri("ndoleroyale"),
        categoryId = CATEGORY_LEGUMES_ID,
        stock = 22,
        description = "Plat à base de légumes"
    ),
    Produit(
        nom = " Ndole Mixte",
        prix = 17.00,
        image = drawableUri("ndolemixe"),
        categoryId = CATEGORY_LEGUMES_ID,
        stock = 22,
        description = "Plat à base de légumes"
    ),
    Produit(
        nom = "Eru Royale",
        prix = 20.00,
        image = drawableUri("eru"),
        categoryId = CATEGORY_LEGUMES_ID,
        stock = 22,
        description = "Plat à base de légumes"
    ),
    Produit(
        nom = "Epinard Sauté",
        prix = 15.00,
        image = drawableUri("epinard"),
        categoryId = CATEGORY_LEGUMES_ID,
        stock = 22,
        description = "Plat à base de légumes"
    ),    Produit(
        nom = "Folon sauté",
        prix = 20.00,
        image = drawableUri("folong"),
        categoryId = CATEGORY_LEGUMES_ID,
        stock = 4,
        description =  "Plat à base de légumes"
    ),
    Produit(
        nom = "Poulet DG",
        prix = 15.00,
        image = drawableUri("pouletdg"),
        categoryId = CATEGORY_LEGUMES_ID,
        stock = 3,
        description =  "Plat à base de légumes"
    ),
    Produit(
        nom = "Sauté de Porc",
        prix = 15.00,
        image = drawableUri("sautedeporc"),
        categoryId = CATEGORY_LEGUMES_ID,
        stock = 2,
        description =  "Plat à base de légumes"
    ),
    Produit(
        nom = "Tripe sauté",
        prix = 15.00,
        image = drawableUri("tripesaute"),
        categoryId = CATEGORY_LEGUMES_ID,
        stock = 2,
        description =  "Plat à base de légumes"
    ),

    // Plat camerounais
    Produit(
        nom = "Taro Royal",
        prix = 20.00,
        image = drawableUri("taro"),
        categoryId = CATEGORY_PLAT_CAMEROUNAIS_ID,
        stock = 2,
        description =  "Plats camerounais",
    ),
    Produit(
        nom = "Taro Viande",
        prix = 20.00,
        image = drawableUri("taroviande"),
        categoryId = CATEGORY_PLAT_CAMEROUNAIS_ID,
        stock = 2,
        description =  "Plats camerounais",
    ),
    Produit(
        nom = "Koki",
        prix = 20.00,
        image = drawableUri("kokis"),
        categoryId = CATEGORY_PLAT_CAMEROUNAIS_ID,
        stock = 2,
        description =  "Plats camerounais",
    ),
    Produit(
        nom = "Kondré Viande",
        prix = 20.00,
        image = drawableUri("kondre"),
        categoryId = CATEGORY_PLAT_CAMEROUNAIS_ID,
        stock = 2,
        description =  "Plats camerounais",
    ),
    Produit(
        nom = "Kondré de Chèvre",
        prix = 25.00,
        image = drawableUri("kondre"),
        categoryId = CATEGORY_PLAT_CAMEROUNAIS_ID,
        stock = 2,
        description =  "Plats camerounais",
    ),
    Produit(
        nom = "Mbongo",
        prix = 20.00,
        image = drawableUri("mbogo"),
        categoryId = CATEGORY_PLAT_CAMEROUNAIS_ID,
        stock = 2,
        description =  "Plats camerounais",
    ),
    Produit(
        nom = "Mbongo",
        prix = 25.00,
        image = drawableUri("mbogo"),
        categoryId = CATEGORY_PLAT_CAMEROUNAIS_ID,
        stock = 2,
        description =  "Plats camerounais",
    ),
)

val sampleVendeursCeny = listOf(
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
