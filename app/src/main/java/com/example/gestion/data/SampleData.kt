package com.example.gestion.data

import com.example.gestion.R
import java.util.UUID


/* =======================
   CATEGORIES
   ======================= */

val CAT_1ER_STOCK = UUID.randomUUID()

val sampleCategories = listOf(
    Category(CAT_1ER_STOCK, "1er STOCK", "Inventaire 1er stock"),

)
val sampleProducts = listOf(

    /* ================= 1er STOCK ================= */
    Produit(nom = "Filtre à gazoil IZUZU", prix = 5000.00, prix_achat = 4500.0, image = R.drawable.manager, categoryId = CAT_1ER_STOCK, stock = 14, description = ""),
    Produit(nom = "Matelas", prix = 100000.00, prix_achat = 45000.0, image = R.drawable.manager, categoryId = CAT_1ER_STOCK, stock = 2, description = ""),
    Produit(nom = "Home cinemas", prix = 250000.00, prix_achat = 155000.0,image = R.drawable.manager, categoryId = CAT_1ER_STOCK, stock = 4, description = ""),

)
val sampleVendeurs = listOf(
    Vendeur(id = 1, nom = "John", prenom = "Doe"),
    Vendeur(id = 2, nom = "Jane", prenom = "Smith"),
    Vendeur(id = 3, nom = "Bob", prenom = "Johnson")
)