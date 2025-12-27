package com.example.oudeika.data

import com.example.oudeika.R
import java.util.UUID


/* =======================
   CATEGORIES
   ======================= */

val CAT_25G = UUID.randomUUID()

val sampleCategories = listOf(
    Category(CAT_25G, "The", "Inventaire 1er stock"),
)

val sampleProducts = listOf(
    /* ================= 1er STOCK ================= */
    Produit(nom = "100g ", prix = 11480.00, image = R.drawable.oudeika, categoryId = CAT_25G, stock = 825, description = ""),
    Produit(nom = "25g ", prix = 11880.00, image = R.drawable.oudeika, categoryId = CAT_25G, stock = 1500, description = ""),
    Produit(nom = "250g", prix = 11480.00, image = R.drawable.oudeika, categoryId = CAT_25G, stock = 825, description = ""),



)
val sampleVendeurs = listOf(
    Vendeur(id = 1, nom = "John", prenom = "Doe"),
    Vendeur(id = 2, nom = "Jane", prenom = "Smith"),
    Vendeur(id = 3, nom = "Bob", prenom = "Johnson")
)