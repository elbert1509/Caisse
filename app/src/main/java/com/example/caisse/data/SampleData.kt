package com.example.caisse.data

import com.example.caisse.R
import java.util.UUID


/* =======================
   CATEGORIES
   ======================= */

val CAT_1ER_STOCK = UUID.randomUUID()
val CAT_2EME_STOCKS = UUID.randomUUID()
val CAT_3EME_STOCKS = UUID.randomUUID()
val CAT_4EME_STOCKS = UUID.randomUUID()
val CAT_5EME_STOCKS = UUID.randomUUID()
val CAT_PIECES_IZUZU = UUID.randomUUID()
val CAT_PIECES_CARINA = UUID.randomUUID()
val CAT_PNEUS = UUID.randomUUID()
val CAT_PNEUS_NOUVEAU = UUID.randomUUID()
val CAT_PIECES_OCCASION_IZUZU = UUID.randomUUID()
val CAT_NOUVEAU_STOCK_PNEUS_2 = UUID.randomUUID()

val sampleCategories = listOf(
    Category(CAT_1ER_STOCK, "1er STOCK", "Inventaire 1er stock"),
    Category(CAT_2EME_STOCKS, "2eme STOCKS", "Inventaire 2eme stocks"),
    Category(CAT_3EME_STOCKS, "3eme STOCKS", "Inventaire 3eme stocks"),
    Category(CAT_4EME_STOCKS, "4eme STOCKS", "Inventaire 4eme stocks"),
    Category(CAT_5EME_STOCKS, "5eme STOCKS", "Inventaire 5eme stocks"),
    Category(CAT_PIECES_IZUZU, "PIECES IZUZU", "Inventaire pièces Izuzu"),
    Category(CAT_PIECES_CARINA, "PIECES CARINA", "Inventaire pièces Carina"),
    Category(CAT_PNEUS, "PNEUS", "Inventaire pneus"),
    Category(CAT_PNEUS_NOUVEAU, "PNEUS NOUVEAU", "Nouveau stock pneus"),
    Category(CAT_PIECES_OCCASION_IZUZU, "PIECES OCCASION IZUZU", "Pièces occasion Izuzu"),
    Category(CAT_NOUVEAU_STOCK_PNEUS_2, "NOUVEA STOCK PNEUS 2", "Deuxième nouveau stock pneus")
)
val sampleProducts = listOf(

    /* ================= 1er STOCK ================= */
    Produit(nom = "Filtre à gazoil IZUZU", prix = 5000.00, image = R.drawable.photo, categoryId = CAT_1ER_STOCK, stock = 141, description = ""),
    Produit(nom = "Matelas", prix = 100000.00, image = R.drawable.photo, categoryId = CAT_1ER_STOCK, stock = 2, description = ""),
    Produit(nom = "Home cinemas", prix = 250000.00, image = R.drawable.photo, categoryId = CAT_1ER_STOCK, stock = 4, description = ""),

    /* ================= 2eme STOCKS ================= */
    Produit(nom = "Pistons", prix = 30000.00, image = R.drawable.photo, categoryId = CAT_2EME_STOCKS, stock = 4, description = ""),
    Produit(nom = "Cardans", prix = 10000.00, image = R.drawable.photo, categoryId = CAT_2EME_STOCKS, stock = 20, description = ""),
    Produit(nom = "Croix de distribution", prix = 15000.00, image = R.drawable.photo, categoryId = CAT_2EME_STOCKS, stock = 3, description = ""),
    Produit(nom = "Pompes à huile", prix = 15000.00, image = R.drawable.photo, categoryId = CAT_2EME_STOCKS, stock = 3, description = ""),
    Produit(nom = "Cuissiniere de biélles", prix = 10000.00, image = R.drawable.photo, categoryId = CAT_2EME_STOCKS, stock = 86, description = ""),
    Produit(nom = "Plaquettes", prix = 6000.00, image = R.drawable.photo, categoryId = CAT_2EME_STOCKS, stock = 7, description = ""),
    Produit(nom = "Cuissiniere de paliés", prix = 10000.00, image = R.drawable.photo, categoryId = CAT_2EME_STOCKS, stock = 4, description = ""),
    Produit(nom = "Joint cv de croisson", prix = 50000.00, image = R.drawable.photo, categoryId = CAT_2EME_STOCKS, stock = 50, description = ""),

    /* ================= 3eme STOCKS ================= */
    Produit(nom = "Radiateur", prix = 50000.00, image = R.drawable.photo, categoryId = CAT_3EME_STOCKS, stock = 4, description = ""),
    Produit(nom = "Soufflés cardans", prix = 2000.00, image = R.drawable.photo, categoryId = CAT_3EME_STOCKS, stock = 35, description = ""),
    Produit(nom = "Barre de stabilisation", prix = 4000.00, image = R.drawable.photo, categoryId = CAT_3EME_STOCKS, stock = 40, description = ""),
    Produit(nom = "Disque d’embrayages", prix = 25000.00, image = R.drawable.photo, categoryId = CAT_3EME_STOCKS, stock = 3, description = ""),
    Produit(nom = "Pompe à eau", prix = 23000.00, image = R.drawable.photo, categoryId = CAT_3EME_STOCKS, stock = 24, description = ""),
    Produit(nom = "Maitre cylindre d’embrayages", prix = 10000.00, image = R.drawable.photo, categoryId = CAT_3EME_STOCKS, stock = 8, description = ""),
    Produit(nom = "Cordeyons", prix = 7000.00, image = R.drawable.photo, categoryId = CAT_3EME_STOCKS, stock = 67, description = ""),
    Produit(nom = "Arret d’huile simple", prix = 3000.00, image = R.drawable.photo, categoryId = CAT_3EME_STOCKS, stock = 69, description = ""),
    Produit(nom = "Arret d’huile soupapes", prix = 10000.00, image = R.drawable.photo, categoryId = CAT_3EME_STOCKS, stock = 40, description = ""),
    Produit(nom = "Roulements", prix = 10000.00, image = R.drawable.photo, categoryId = CAT_3EME_STOCKS, stock = 2, description = ""),
    Produit(nom = "Joint de culasse en cartons", prix = 10000.00, image = R.drawable.photo, categoryId = CAT_3EME_STOCKS, stock = 5, description = ""),

    /* ================= 4eme STOCKS ================= */
    Produit(nom = "Amortisseurs", prix = 14000.00, image = R.drawable.photo, categoryId = CAT_4EME_STOCKS, stock = 4, description = ""),
    Produit(nom = "Maitre cylindre de frein", prix = 15000.00, image = R.drawable.photo, categoryId = CAT_4EME_STOCKS, stock = 6, description = ""),
    Produit(nom = "Pot de farres", prix = 20000.00, image = R.drawable.photo, categoryId = CAT_4EME_STOCKS, stock = 20, description = ""),
    Produit(nom = "Filtre à carburant", prix = 3500.00, image = R.drawable.photo, categoryId = CAT_4EME_STOCKS, stock = 166, description = ""),
    Produit(nom = "Segments", prix = 50000.00, image = R.drawable.photo, categoryId = CAT_4EME_STOCKS, stock = 2, description = ""),
    Produit(nom = "Nécessaire d’embrayages", prix = 4000.00, image = R.drawable.photo, categoryId = CAT_4EME_STOCKS, stock = 101, description = ""),
    Produit(nom = "Essuie glaces", prix = 4000.00, image = R.drawable.photo, categoryId = CAT_4EME_STOCKS, stock = 10, description = ""),
    Produit(nom = "Garnitures turbo", prix = 7000.00, image = R.drawable.photo, categoryId = CAT_4EME_STOCKS, stock = 37, description = ""),

    /* ================= 5eme STOCKS ================= */
    Produit(nom = "Disque d’embrayages", prix = 25000.00, image = R.drawable.photo, categoryId = CAT_5EME_STOCKS, stock = 20, description = ""),
    Produit(nom = "Plateaux", prix = 30000.00, image = R.drawable.photo, categoryId = CAT_5EME_STOCKS, stock = 4, description = ""),
    Produit(nom = "Tambours", prix = 10000.00, image = R.drawable.photo, categoryId = CAT_5EME_STOCKS, stock = 17, description = ""),
    Produit(nom = "Cylindre de roux", prix = 5000.00, image = R.drawable.photo, categoryId = CAT_5EME_STOCKS, stock = 131, description = ""),
    Produit(nom = "Support moteurs", prix = 10000.00, image = R.drawable.photo, categoryId = CAT_5EME_STOCKS, stock = 9, description = ""),
    Produit(nom = "Soupapes", prix = 8750.00, image = R.drawable.photo, categoryId = CAT_5EME_STOCKS, stock = 8, description = ""),
    Produit(nom = "Joints de culasse", prix = 7000.00, image = R.drawable.photo, categoryId = CAT_5EME_STOCKS, stock = 52, description = ""),
    Produit(nom = "Roulements", prix = 10000.00, image = R.drawable.photo, categoryId = CAT_5EME_STOCKS, stock = 8, description = ""),
    Produit(nom = "Graisses", prix = 2000.00, image = R.drawable.photo, categoryId = CAT_5EME_STOCKS, stock = 29, description = ""),
    Produit(nom = "Amortisseurs", prix = 14000.00, image = R.drawable.photo, categoryId = CAT_5EME_STOCKS, stock = 5, description = ""),
    Produit(nom = "Huile assistés", prix = 2000.00, image = R.drawable.photo, categoryId = CAT_5EME_STOCKS, stock = 30, description = ""),
    Produit(nom = "Crémallieres", prix = 90000.00, image = R.drawable.photo, categoryId = CAT_5EME_STOCKS, stock = 11, description = ""),
    Produit(nom = "Croisson turbo", prix = 5000.00, image = R.drawable.photo, categoryId = CAT_5EME_STOCKS, stock = 39, description = ""),

    /* ================= PIECES IZUZU ================= */
    Produit(nom = "Sement de piston", prix = 78000.00, image = R.drawable.photo, categoryId = CAT_PIECES_IZUZU, stock = 10, description = ""),
    Produit(nom = "Rotures de direction", prix = 35000.00, image = R.drawable.photo, categoryId = CAT_PIECES_IZUZU, stock = 25, description = ""),
    Produit(nom = "Filtre à huile", prix = 5000.00, image = R.drawable.photo, categoryId = CAT_PIECES_IZUZU, stock = 43, description = ""),
    Produit(nom = "Cylindre bloc de lame", prix = 10000.00, image = R.drawable.photo, categoryId = CAT_PIECES_IZUZU, stock = 3, description = ""),
    Produit(nom = "Pugnion complet", prix = 150000.00, image = R.drawable.photo, categoryId = CAT_PIECES_IZUZU, stock = 7, description = ""),
    Produit(nom = "Roulements AV ET AR", prix = 10000.00, image = R.drawable.photo, categoryId = CAT_PIECES_IZUZU, stock = 90, description = ""),
    Produit(nom = "Cylindre de roux", prix = 15000.00, image = R.drawable.photo, categoryId = CAT_PIECES_IZUZU, stock = 40, description = ""),
    Produit(nom = "Garniture de frein", prix = 25000.00, image = R.drawable.photo, categoryId = CAT_PIECES_IZUZU, stock = 49, description = ""),
    Produit(nom = "Amortisseusrs", prix = 0.00, image = R.drawable.photo, categoryId = CAT_PIECES_IZUZU, stock = 50, description = ""),
    Produit(nom = "Relais de cardans FTR", prix = 45000.00, image = R.drawable.photo, categoryId = CAT_PIECES_IZUZU, stock = 15, description = ""),
    Produit(nom = "Bouchons de radiateur", prix = 0.00, image = R.drawable.photo, categoryId = CAT_PIECES_IZUZU, stock = 90, description = ""),
    Produit(nom = "Disques d’embrayages", prix = 120000.00, image = R.drawable.photo, categoryId = CAT_PIECES_IZUZU, stock = 2, description = ""),
    Produit(nom = "Pompe à huile", prix = 120000.00, image = R.drawable.photo, categoryId = CAT_PIECES_IZUZU, stock = 4, description = ""),
    Produit(nom = "Segment pour 6 MOTEUR", prix = 0.00, image = R.drawable.photo, categoryId = CAT_PIECES_IZUZU, stock = 6, description = ""),
    Produit(nom = "Cuisinier de palliés (10, raison de 2 paire)", prix = 70000.00, image = R.drawable.photo, categoryId = CAT_PIECES_IZUZU, stock = 1, description = ""),
    Produit(nom = "Arret d’huile noeud de pont", prix = 10000.00, image = R.drawable.photo, categoryId = CAT_PIECES_IZUZU, stock = 30, description = ""),

    // Suite (page suivante – mêmes colonnes)
    Produit(nom = "Croisson de cardans MPR", prix = 45000.00, image = R.drawable.photo, categoryId = CAT_PIECES_IZUZU, stock = 2, description = ""),
    Produit(nom = "Coupelles de frein", prix = 15000.00, image = R.drawable.photo, categoryId = CAT_PIECES_IZUZU, stock = 5, description = ""),
    Produit(nom = "Cales laterale", prix = 25000.00, image = R.drawable.photo, categoryId = CAT_PIECES_IZUZU, stock = 5, description = ""),
    Produit(nom = "Tétons de demi arbre", prix = 30000.00, image = R.drawable.photo, categoryId = CAT_PIECES_IZUZU, stock = 3, description = ""),
    Produit(nom = "Cuisinier d’arbre à came", prix = 0.00, image = R.drawable.photo, categoryId = CAT_PIECES_IZUZU, stock = 0, description = ""), // prix/stock non renseignés
    Produit(nom = "Arret d’huile volant moteur", prix = 0.00, image = R.drawable.photo, categoryId = CAT_PIECES_IZUZU, stock = 0, description = ""), // prix/stock non renseignés
    Produit(nom = "Arret d’huile coulis", prix = 0.00, image = R.drawable.photo, categoryId = CAT_PIECES_IZUZU, stock = 0, description = ""), // prix/stock non renseignés
    Produit(nom = "Cuisinier de bielle", prix = 0.00, image = R.drawable.photo, categoryId = CAT_PIECES_IZUZU, stock = 0, description = ""), // prix/stock non renseignés
    Produit(nom = "Axe de fussé", prix = 100000.00, image = R.drawable.photo, categoryId = CAT_PIECES_IZUZU, stock = 4, description = ""),


    /* ================= PIECES CARINA (6eme STOCKS) ================= */
    Produit(nom = "Crémalliere", prix = 90000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 8, description = ""),
    Produit(nom = "Segment", prix = 55000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 19, description = ""),
    Produit(nom = "Verre carina et CAE 10tu0", prix = 6000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 59, description = ""),
    Produit(nom = "Moteurs assistés", prix = 50000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 16, description = ""),
    Produit(nom = "Cole 2 EN 1", prix = 2000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 23, description = ""),
    Produit(nom = "Plaquettes", prix = 6000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 37, description = ""),
    Produit(nom = "Rotule de direction", prix = 4000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 43, description = ""),
    Produit(nom = "Croisons joint cv", prix = 5000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 87, description = ""),
    Produit(nom = "Maitre cylindre freins", prix = 20000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 10, description = ""),
    Produit(nom = "Support moteurs AR", prix = 15000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 15, description = ""),
    Produit(nom = "Biéllettes de stabilisation", prix = 4000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 48, description = ""),
    Produit(nom = "Veilleuses carina E", prix = 7000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 34, description = ""),
    Produit(nom = "Soufflés cardans", prix = 2000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 95, description = ""),
    Produit(nom = "Filtres à airs turbo", prix = 5000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 48, description = ""),
    Produit(nom = "Amortisseurs", prix = 15000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 20, description = ""),
    Produit(nom = "Croix de distribution", prix = 15000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 10, description = ""),
    Produit(nom = "Cuissiniére de bielle", prix = 10000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 38, description = ""),


    /* ================= PNEUS ================= */
    Produit(nom = "KINGBOSS 185/70R14 88T 4 G521", prix = 25000.00, image = R.drawable.photo, categoryId = CAT_PNEUS, stock = 93, description = ""),
    Produit(nom = "KINGBOSS 195/70R14 91T 4 G521", prix = 26000.00, image = R.drawable.photo, categoryId = CAT_PNEUS, stock = 93, description = ""),
    Produit(nom = "KINGBOSS 215/70R15C 109/107T 8 G326", prix = 35000.00, image = R.drawable.photo, categoryId = CAT_PNEUS, stock = 144, description = ""),
    Produit(nom = "KINGBOSS 265/70R16 112H 4 G577", prix = 50000.00, image = R.drawable.photo, categoryId = CAT_PNEUS, stock = 152, description = ""),
    Produit(nom = "KINGBOSS P245/75R16 109T 4 GT02", prix = 50000.00, image = R.drawable.photo, categoryId = CAT_PNEUS, stock = 150, description = ""),
    Produit(nom = "KINGBOS 225/70R15C 112/110R 8 G326", prix = 50000.00, image = R.drawable.photo, categoryId = CAT_PNEUS, stock = 122, description = ""),


    /* ================= PNEUS NOUVEAU ================= */
    Produit(nom = "ROADBOSS 750R16LT 122/118K", prix = 80000.00, image = R.drawable.photo, categoryId = CAT_PNEUS_NOUVEAU, stock = 310, description = ""),
    Produit(nom = "KINBOSS 205R14C 107/105R 8 325", prix = 35000.00, image = R.drawable.photo, categoryId = CAT_PNEUS_NOUVEAU, stock = 185, description = ""),
    Produit(nom = "KINBOSS 195/65R15 91H 4", prix = 30000.00, image = R.drawable.photo, categoryId = CAT_PNEUS_NOUVEAU, stock = 152, description = ""),


    /* ================= PIECES OCCASION IZUZU ================= */
    Produit(nom = "LAXE SIMPLE", prix = 650000.00, image = R.drawable.photo, categoryId = CAT_PIECES_OCCASION_IZUZU, stock = 0, description = ""), // reste en stock non renseigné
    Produit(nom = "PONTS", prix = 480000.00, image = R.drawable.photo, categoryId = CAT_PIECES_OCCASION_IZUZU, stock = 0, description = ""), // reste en stock non renseigné
    Produit(nom = "BOITTES", prix = 450000.00, image = R.drawable.photo, categoryId = CAT_PIECES_OCCASION_IZUZU, stock = 0, description = ""), // reste en stock non renseigné
    Produit(nom = "MOTEUR COMPLET", prix = 3700000.00, image = R.drawable.photo, categoryId = CAT_PIECES_OCCASION_IZUZU, stock = 0, description = ""), // reste en stock non renseigné
    Produit(nom = "LECTEURS", prix = 0.00, image = R.drawable.photo, categoryId = CAT_PIECES_OCCASION_IZUZU, stock = 0, description = ""), // prix + reste en stock non renseignés
    Produit(nom = "MATELAS UNE PLACE", prix = 0.00, image = R.drawable.photo, categoryId = CAT_PIECES_OCCASION_IZUZU, stock = 0, description = ""), // prix + reste en stock non renseignés
    Produit(nom = "MATELAS 2 PLACES", prix = 80000.00, image = R.drawable.photo, categoryId = CAT_PIECES_OCCASION_IZUZU, stock = 0, description = ""), // reste en stock non renseigné
    Produit(nom = "MATELAS 3 PLACES", prix = 100000.00, image = R.drawable.photo, categoryId = CAT_PIECES_OCCASION_IZUZU, stock = 0, description = ""), // reste en stock non renseigné


    /* ================= PIECES CARINA (7eme STOCKS) ================= */
    Produit(nom = "MACHOIRE DE FREIN", prix = 7000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 101, description = ""),
    Produit(nom = "CREMAILLERE DE DIRECTION", prix = 90000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 13, description = ""),
    Produit(nom = "TAMBOUR DE FREIN", prix = 12000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 43, description = ""),
    Produit(nom = "DISQUE DE FREIN CARINA E", prix = 12000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 41, description = ""),
    Produit(nom = "DISQUE DE FREIN CAE 100", prix = 12000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 59, description = ""),
    Produit(nom = "ROULEMENT TENDEUR 2C", prix = 10000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 87, description = ""),
    Produit(nom = "PISTON 13101.64141", prix = 35000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 17, description = ""),
    Produit(nom = "PISTON 13103.64141", prix = 35000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 18, description = ""),
    Produit(nom = "LIASON STABILISATRICE CLT.2 RR", prix = 4000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 80, description = ""),
    Produit(nom = "COURROIE DE DISTRIBUTION 2C", prix = 15000.00, image = R.drawable.photo, categoryId = CAT_PIECES_CARINA, stock = 118, description = ""),


    /* ================= NOUVEA STOCK PNEUS 2 ================= */
    Produit(nom = "PNEUS LONGMARCH 750R16LT", prix = 90000.00, image = R.drawable.photo, categoryId = CAT_NOUVEAU_STOCK_PNEUS_2, stock = 289, description = ""),
    Produit(nom = "PNEUS KINGBOSS 185/70R14", prix = 25000.00, image = R.drawable.photo, categoryId = CAT_NOUVEAU_STOCK_PNEUS_2, stock = 198, description = ""),
    Produit(nom = "PNEUS KINGBOSS 195/70R14", prix = 26000.00, image = R.drawable.photo, categoryId = CAT_NOUVEAU_STOCK_PNEUS_2, stock = 161, description = ""),
    Produit(nom = "PNEUS KINGBOSS 195/65R16", prix = 30000.00, image = R.drawable.photo, categoryId = CAT_NOUVEAU_STOCK_PNEUS_2, stock = 136, description = "")




)
val sampleVendeurs = listOf(
    Vendeur(id = 1, nom = "John", prenom = "Doe"),
    Vendeur(id = 2, nom = "Jane", prenom = "Smith"),
    Vendeur(id = 3, nom = "Bob", prenom = "Johnson")
)