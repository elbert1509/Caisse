package com.example.caisse.data

import com.example.caisse.R
import java.util.UUID


val CATEGORY_VIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111")
val CATEGORY_DRINKS_ID = UUID.fromString("22222222-2222-2222-2222-222222222222")
val CATEGORY_POISSON_ID = UUID.fromString("33333333-3333-3333-3333-333333333333")
val CATEGORY_VIANDE_ID  = UUID.fromString("44444444-4444-4444-4444-444444444444")
val CATEGORY_SOUPE_ID  = UUID.fromString("44444444-4444-4444-2222-444444444444")
val CATEGORY_SAUCE_ID  = UUID.fromString("44444444-4444-4444-3333-444444444444")
val CATEGORY_COMPLEMENT_ID  = UUID.fromString("44444444-4444-4444-0000-444444444444")

val sampleCategories = listOf(
    Category(id = CATEGORY_VIN_ID, name = "Vin", description = "Repas et plats principaux"),
    Category(id = CATEGORY_DRINKS_ID, name = "Biere", description = "Boissons"),
    Category(id = CATEGORY_POISSON_ID, name = "Poisson", description = "Pâtisseries et douceurs"),
    Category(id = CATEGORY_VIANDE_ID , name = "Viande", description = "Entrées"),
    Category(id = CATEGORY_SOUPE_ID , name = "Soupe", description = "Entrées"),
    Category(id = CATEGORY_SAUCE_ID , name = "Sauce", description = "Entrées"),
    Category(id = CATEGORY_COMPLEMENT_ID , name = "Accompagnement", description = "Entrées")
)
val sampleProducts = listOf(

    // Bière
    Produit(nom = "Guiness G", prix = 10.00, image = R.drawable.guiness, categoryId = CATEGORY_DRINKS_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "Guiness P", prix = 5.00, image = R.drawable.guiness, categoryId = CATEGORY_DRINKS_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "Leffe G", prix = 8.00, image = R.drawable.leffe, categoryId = CATEGORY_DRINKS_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "Leffe P ", prix = 4.00, image = R.drawable.leffe, categoryId = CATEGORY_DRINKS_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "Leffe Formule", prix = 12.00, image = R.drawable.leffeform, categoryId = CATEGORY_DRINKS_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "1664 G", prix = 10.00, image = R.drawable.r1664, categoryId = CATEGORY_DRINKS_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "1664 P", prix = 10.00, image = R.drawable.r1664, categoryId = CATEGORY_DRINKS_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "1664 Formule", prix = 10.00, image = R.drawable.r16form, categoryId = CATEGORY_DRINKS_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "Desperados G", prix = 8.00, image = R.drawable.despe, categoryId = CATEGORY_DRINKS_ID, stock = 35, description = "Jus de pommes frais"),
    Produit(nom = "Desperados P", prix = 5.00, image = R.drawable.despe, categoryId = CATEGORY_DRINKS_ID, stock = 35, description = "Jus de pommes frais"),
    Produit(nom = "Desperados Formule", prix = 12.00, image = R.drawable.despeform, categoryId = CATEGORY_DRINKS_ID, stock = 35, description = "Jus de pommes frais"),
    Produit(nom = "Castel", prix = 8.00, image = R.drawable.castel, categoryId = CATEGORY_DRINKS_ID, stock = 35, description = "Jus de pommes frais"),
    Produit(nom = "33 Export", prix = 8.00, image = R.drawable.export, categoryId = CATEGORY_DRINKS_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "Kandji", prix = 8.00, image = R.drawable.export, categoryId = CATEGORY_DRINKS_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "Whisky kola", prix = 8.00, image = R.drawable.boostercola, categoryId = CATEGORY_DRINKS_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "Top", prix = 6.00, image = R.drawable.top, categoryId = CATEGORY_DRINKS_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "Djino", prix = 6.00, image = R.drawable.djino, categoryId = CATEGORY_DRINKS_ID, stock = 22, description = "Milkshake à la vanille"),

    // Vin

    Produit(nom = "Moelleux ", prix = 20.00, image = R.drawable.vinblanc, categoryId = CATEGORY_VIN_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "Cabernet d'Anjou ", prix = 15.00, image = R.drawable.cabernet, categoryId = CATEGORY_VIN_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "Côte de Rhone ", prix = 20.00, image = R.drawable.cabernet, categoryId = CATEGORY_VIN_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "Côte de Provence ", prix = 20.00, image = R.drawable.cabernet, categoryId = CATEGORY_VIN_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "Mouton Cadet ", prix = 20.00, image = R.drawable.moutoncadet, categoryId = CATEGORY_VIN_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "Saint Emilion ", prix = 30.00, image = R.drawable.saintemilion, categoryId = CATEGORY_VIN_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "Vin Bordeaux ", prix = 20.00, image = R.drawable.vinbordeaux, categoryId = CATEGORY_VIN_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "Vin Bordeaux P ", prix = 10.00, image = R.drawable.vinbordeaux, categoryId = CATEGORY_VIN_ID, stock = 22, description = "Milkshake à la vanille"),
    Produit(nom = "Vin Bordeaux P ", prix = 15.00, image = R.drawable.vinbordeaux, categoryId = CATEGORY_VIN_ID, stock = 22, description = "Milkshake à la vanille"),

    // Grillade Poison
    Produit(nom = "Malangwa", prix = 20.00, image = R.drawable.poisson1, categoryId = CATEGORY_POISSON_ID, stock = 22, description = "Grillade de Poisson"),
    Produit(nom = "Sole", prix = 40.00, image = R.drawable.poisson1, categoryId = CATEGORY_POISSON_ID, stock = 22, description = "Grillade de Poisson"),
    Produit(nom = "Capitaine ", prix = 20.00, image = R.drawable.poisson1, categoryId = CATEGORY_POISSON_ID, stock = 22, description = "Grillade de Poisson"),
    Produit(nom = "Capitaine ", prix = 25.00, image = R.drawable.poisson1, categoryId = CATEGORY_POISSON_ID, stock = 22, description = "Grillade de Poisson"),
    Produit(nom = "Capitaine ", prix = 30.00, image = R.drawable.poisson1, categoryId = CATEGORY_POISSON_ID, stock = 22, description = "Grillade de Poisson"),
    Produit(nom = "Maquereau", prix = 15.00, image = R.drawable.poisson1, categoryId = CATEGORY_POISSON_ID, stock = 22, description = "Grillade de Poisson"),
    Produit(nom = "Maquereau", prix = 20.00, image = R.drawable.poisson1, categoryId = CATEGORY_POISSON_ID, stock = 22, description = "Grillade de Poisson"),
    Produit(nom = "Maquereau", prix = 25.00, image = R.drawable.poisson1, categoryId = CATEGORY_POISSON_ID, stock = 22, description = "Grillade de Poisson"),
    Produit(nom = "Tilapia", prix = 15.00, image = R.drawable.poisson1, categoryId = CATEGORY_POISSON_ID, stock = 22, description = "Grillade de Poisson"),
    Produit(nom = "Tilapia", prix = 20.00, image = R.drawable.poisson1, categoryId = CATEGORY_POISSON_ID, stock = 22, description = "Grillade de Poisson"),


    // Grillade Viande
    Produit(nom = "Brochettes", prix = 10.00, image = R.drawable.brochette, categoryId = CATEGORY_VIANDE_ID, stock = 22, description = "Grillade de Poisson"),
    Produit(nom = "Porc Braisé", prix = 10.00, image = R.drawable.porc, categoryId = CATEGORY_VIANDE_ID, stock = 22, description = "Grillade de Poisson"),
    Produit(nom = "Gesier ", prix = 10.00, image = R.drawable.porc, categoryId = CATEGORY_VIANDE_ID, stock = 22, description = "Grillade de Poisson"),
    Produit(nom = "Poulet Braisé ", prix = 10.00, image = R.drawable.poulet, categoryId = CATEGORY_VIANDE_ID, stock = 22, description = "Grillade de Poisson"),
    Produit(nom = "Aile de Poulet ", prix = 10.00, image = R.drawable.aile, categoryId = CATEGORY_VIANDE_ID, stock = 22, description = "Grillade de Poisson"),


    // Soupe
    Produit(nom = "Bouillon de Poisson", prix = 15.00, image = R.drawable.soupe, categoryId = CATEGORY_SOUPE_ID, stock = 22, description = "Soupe de Poisson"),
    Produit(nom = "Bouillon de Poisson", prix = 20.00, image = R.drawable.soupe, categoryId = CATEGORY_SOUPE_ID, stock = 22, description = "Soupe de Poisson"),
    Produit(nom = "Bouillon de Poisson", prix = 25.00, image = R.drawable.soupe, categoryId = CATEGORY_SOUPE_ID, stock = 22, description = "Soupe de Poisson"),
    Produit(nom = "Bouillon de Boeuf ", prix = 15.00, image = R.drawable.soupe, categoryId = CATEGORY_SOUPE_ID, stock = 22, description = "Soupe de Poisson"),
    Produit(nom = "Bouillon de Porc", prix = 15.00, image = R.drawable.soupe, categoryId = CATEGORY_SOUPE_ID, stock = 22, description = "Soupe de Poisson"),
    Produit(nom = "Rôti de Porc", prix = 15.00, image = R.drawable.soupe, categoryId = CATEGORY_SOUPE_ID, stock = 22, description = "Soupe de Poisson"),


    // Sauce
    Produit(nom = "Sauce Tomate ", prix = 15.00, image = R.drawable.soupe, categoryId = CATEGORY_SAUCE_ID, stock = 22, description = "Grillade de Poisson"),
    Produit(nom = "Maffe Viande ", prix = 15.00, image = R.drawable.maffe, categoryId = CATEGORY_SAUCE_ID, stock = 22, description = "Grillade de Poisson"),
    Produit(nom = "Maffe Poisson fumé ", prix = 15.00, image = R.drawable.maffe, categoryId = CATEGORY_SAUCE_ID, stock = 22, description = "Grillade de Poisson"),
    Produit(nom = "Gombo (Viande + Poisson) ", prix = 18.00, image = R.drawable.gombo, categoryId = CATEGORY_SAUCE_ID, stock = 22, description = "Grillade de Poisson"),
    Produit(nom = "Gombo Poisson fumé ", prix = 15.00, image = R.drawable.gombo, categoryId = CATEGORY_SAUCE_ID, stock = 22, description = "Grillade de Poisson"),
    Produit(nom = "Yassa Poulet ", prix = 15.00, image = R.drawable.yassa, categoryId = CATEGORY_SAUCE_ID, stock = 22, description = "Grillade de Poisson"),

    // Compléments
    Produit(nom = " Bâton de Manioc  ", prix = 0.00, image = R.drawable.manioc, categoryId = CATEGORY_COMPLEMENT_ID, stock = 22, description = "Complement de Poisson"),
    Produit(nom = " Miodo  ", prix = 0.00, image = R.drawable.miodo, categoryId = CATEGORY_COMPLEMENT_ID, stock = 22, description = "Complement de Poisson"),
    Produit(nom = " Plantain vapeur   ", prix = 0.00, image = R.drawable.plantainvap, categoryId = CATEGORY_COMPLEMENT_ID, stock = 22, description = "Complement de Poisson"),
    Produit(nom = " Plantain tapé  ", prix = 0.00, image = R.drawable.tape, categoryId = CATEGORY_COMPLEMENT_ID, stock = 22, description = "Complement de Poisson"),
    Produit(nom = " Riz  ", prix = 0.00, image = R.drawable.riz, categoryId = CATEGORY_COMPLEMENT_ID, stock = 22, description = "Complement de Poisson"),
    Produit(nom = " Semoule", prix = 0.00, image = R.drawable.semoule, categoryId = CATEGORY_COMPLEMENT_ID, stock = 22, description = "Complement de Poisson"),
    Produit(nom = " Aloko", prix = 0.00, image = R.drawable.alloco, categoryId = CATEGORY_COMPLEMENT_ID, stock = 22, description = "Complement de Poisson"),
    Produit(nom = " Attieké", prix = 0.00, image = R.drawable.attikie, categoryId = CATEGORY_COMPLEMENT_ID, stock = 22, description = "Complement de Poisson"),
    Produit(nom = " Supplément", prix = 5.00, image = R.drawable.supplement, categoryId = CATEGORY_COMPLEMENT_ID, stock = 22, description = "Complement de Poisson"),

    )
val sampleVendeurs = listOf(
    Vendeur(id = 1, nom = "John", prenom = "Doe"),
    Vendeur(id = 2, nom = "Jane", prenom = "Smith"),
    Vendeur(id = 3, nom = "Bob", prenom = "Johnson")
)