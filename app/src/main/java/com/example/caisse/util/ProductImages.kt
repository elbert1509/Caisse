package com.example.caisse.util

/**
 * Images produit fournies dans res/drawable, proposées comme alternative à la galerie
 * lors de l'ajout / la modification d'un produit.
 *
 * On stocke l'image choisie sous la forme drawableUri(name) ("android.resource://...")
 * — même format que les données d'exemple — ce qui reste affichable par Coil et
 * synchronisable comme une simple chaîne (les drawables sont embarqués dans l'app,
 * donc disponibles à l'identique sur tous les appareils).
 */
val PRODUCT_DRAWABLES: List<String> = listOf(
    // Bières
    "regab", "castel", "beaufort", "doppel", "guiness", "export", "heineken",
    "despe", "despeform", "r1664", "r16form", "leffe", "leffeform", "imperial",
    "xxl", "trecepas", "royal",
    // Sodas / boissons
    "coca", "orangina", "top_orange", "top", "djino", "wordcola", "sumol",
    "youzou", "racine", "schweppes", "boostercola", "chill", "boisson",
    // Vins / champagnes
    "vino", "vinblanc", "vinbordeaux", "moutoncadet", "saintemilion",
    "cabernet", "coterhone", "coteprovence", "moet",
    // Spiritueux / cocktails
    "jack", "vodka", "chivas", "henessy", "baileys", "siredwards", "genef",
    "zombie", "sombrero", "cosmoparc", "aperitif", "cotis",
    // Plats / viandes
    "burger", "burger1", "burger11", "burgerr", "poulet", "pouletdg", "aile",
    "porc", "sautedeporc", "tripesaute", "mbogo", "kondre", "taroviande",
    "brochette", "maffe", "yassa", "ndoleroyale", "ndolemixe", "eru",
    "epinard", "folong", "gombo", "soupe", "kokis", "imaquis",
    // Poissons
    "poisson", "poisson1", "oudeika", "miodo",
    // Accompagnements
    "alloco", "attikie", "manioc", "plantainvap", "riz", "semoule", "igname",
    "taro", "tape", "supplement", "sauces",
    // Desserts
    "dessert"
)
