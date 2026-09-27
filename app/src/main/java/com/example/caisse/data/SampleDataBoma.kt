package com.example.caisse.data

import java.util.UUID

private const val PKG = "com.example.caisse"
private fun drawableUri(name: String) =
    "android.resource://$PKG/drawable/$name"

// Stock initial par défaut (le menu n'indique pas de stock) — à ajuster
private const val STOCK_INITIAL = 20

val CATEGORY_PETITDEJ_BOMA_ID = UUID.fromString("0100d0ee-84d9-47f3-8317-801add0786be")
val CATEGORY_BURGERS_BOMA_ID = UUID.fromString("4f51b8c0-78a9-4908-a470-96bd4244329d")
val CATEGORY_PLATS_BOMA_ID = UUID.fromString("3bdd6396-749b-4100-8d31-9bfe97c2fe93")
val CATEGORY_BROCHETTES_BOMA_ID = UUID.fromString("0023a3fd-6850-42a7-b343-64c58ff3f0b4")
val CATEGORY_VILLAGE_BOMA_ID = UUID.fromString("a150106f-0fc3-464d-a84b-7a57f201c836")
val CATEGORY_ACCOMP_BOMA_ID = UUID.fromString("dd8e919a-723e-4481-bbbd-106b714cb3b6")
val CATEGORY_SOFTS_BOMA_ID = UUID.fromString("5ecb1ea0-64ae-442b-857b-b6b9771baec6")
val CATEGORY_BIERES_BOMA_ID = UUID.fromString("6cf1d140-444e-40c4-9111-6a525acd754b")
val CATEGORY_VINS_BOMA_ID = UUID.fromString("bdfc961a-76f2-4e2d-aa5e-d8607479a9da")
val CATEGORY_LIQUEURS_BOMA_ID = UUID.fromString("7bedad70-3744-46e8-af60-6c9f4d49fbef")

val sampleCategoriesBoma = listOf(
    Category(
        id = CATEGORY_PETITDEJ_BOMA_ID,
        name = "Petit-déjeuner",
        description = "Boissons chaudes et œufs"
    ),
    Category(
        id = CATEGORY_BURGERS_BOMA_ID,
        name = "Burgers",
        description = "Burgers servis avec frites"
    ),
    Category(
        id = CATEGORY_PLATS_BOMA_ID,
        name = "Plats",
        description = "Grillades, braisés et salades"
    ),
    Category(
        id = CATEGORY_BROCHETTES_BOMA_ID,
        name = "Brochettes",
        description = "Brochettes grillées"
    ),
    Category(
        id = CATEGORY_VILLAGE_BOMA_ID,
        name = "Comme au village",
        description = "Plat du jour avec accompagnement"
    ),
    Category(
        id = CATEGORY_ACCOMP_BOMA_ID,
        name = "Accompagnements",
        description = "Riz, manioc, légumes et féculents"
    ),
    Category(
        id = CATEGORY_SOFTS_BOMA_ID,
        name = "Boissons non alcoolisées",
        description = "Eaux, sodas, jus et sirops"
    ),
    Category(
        id = CATEGORY_BIERES_BOMA_ID,
        name = "Bières",
        description = "Bières et boissons alcoolisées"
    ),
    Category(
        id = CATEGORY_VINS_BOMA_ID,
        name = "Vins",
        description = "Vins rouges et blancs"
    ),
    Category(
        id = CATEGORY_LIQUEURS_BOMA_ID,
        name = "Liqueurs",
        description = "Liqueurs à la dose ou à la bouteille (75cl)"
    )
)

val sampleProductsBoma = listOf(

    // ---------- PETIT-DÉJEUNER ----------
    Produit(
        nom = "Café",
        prix = 2000.00,
        image = drawableUri("cafe"),
        categoryId = CATEGORY_PETITDEJ_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Café chaud"
    ),

    Produit(
        nom = "Lait chaud",
        prix = 2000.00,
        image = drawableUri("lait_chaud"),
        categoryId = CATEGORY_PETITDEJ_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Lait chaud"
    ),

    Produit(
        nom = "Thé",
        prix = 2000.00,
        image = drawableUri("the"),
        categoryId = CATEGORY_PETITDEJ_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Thé chaud"
    ),

    Produit(
        nom = "Omelette",
        prix = 2500.00,
        image = drawableUri("omelette"),
        categoryId = CATEGORY_PETITDEJ_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Omelette"
    ),

    Produit(
        nom = "Œuf au plat",
        prix = 2500.00,
        image = drawableUri("oeuf_au_plat"),
        categoryId = CATEGORY_PETITDEJ_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Œufs au plat"
    ),

    // ---------- BURGERS ----------
    Produit(
        nom = "Cheese burger",
        prix = 3500.00,
        image = drawableUri("cheese_burger"),
        categoryId = CATEGORY_BURGERS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Cheese burger avec frites"
    ),

    Produit(
        nom = "Burger à cheval",
        prix = 4500.00,
        image = drawableUri("burger_a_cheval"),
        categoryId = CATEGORY_BURGERS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Burger avec œuf, servi avec frites"
    ),

    // ---------- PLATS ----------
    Produit(
        nom = "Coupés-coupés BOMA",
        prix = 2000.00,
        image = drawableUri("coupe_coupe"),
        categoryId = CATEGORY_PLATS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Bœuf, tripes, foie, rognons — à partir de 2000"
    ),

    Produit(
        nom = "Cuisse de poulet braisée",
        prix = 2000.00,
        image = drawableUri("cuisse_poulet"),
        categoryId = CATEGORY_PLATS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Cuisse de poulet braisée"
    ),

    Produit(
        nom = "Saucisse de Toulouse",
        prix = 2000.00,
        image = drawableUri("saucisse_toulouse"),
        categoryId = CATEGORY_PLATS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Saucisse de Toulouse grillée"
    ),

    Produit(
        nom = "Salade niçoise",
        prix = 3000.00,
        image = drawableUri("salade_nicoise"),
        categoryId = CATEGORY_PLATS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Salade niçoise"
    ),

    Produit(
        nom = "Salade avocat-crevettes",
        prix = 3000.00,
        image = drawableUri("salade_avocat_crevettes"),
        categoryId = CATEGORY_PLATS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Salade avocat et crevettes"
    ),

    Produit(
        nom = "Salade composée",
        prix = 3000.00,
        image = drawableUri("salade_avocat_crevettes"),
        categoryId = CATEGORY_PLATS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Salade composée"
    ),

    Produit(
        nom = "Ribs de mouton BOMA",
        prix = 3500.00,
        image = drawableUri("ribs_mouton"),
        categoryId = CATEGORY_PLATS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Ribs de mouton maison"
    ),

    Produit(
        nom = "Poisson braisé",
        prix = 5000.00,
        image = drawableUri("poisson1"),
        categoryId = CATEGORY_PLATS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Poisson braisé — à partir de 5000"
    ),

    Produit(
        nom = "Côtes de bœuf braisées",
        prix = 6000.00,
        image = drawableUri("cote_boeuf"),
        categoryId = CATEGORY_PLATS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Côtes de bœuf braisées avec accompagnement"
    ),

    // ---------- BROCHETTES ----------
    Produit(
        nom = "Brochettes de rognons",
        prix = 1000.00,
        image = drawableUri("brochette_rognons"),
        categoryId = CATEGORY_BROCHETTES_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Brochettes de rognons"
    ),

    Produit(
        nom = "Brochettes de poulet",
        prix = 1500.00,
        image = drawableUri("brochette_poulet"),
        categoryId = CATEGORY_BROCHETTES_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Brochettes de poulet"
    ),

    Produit(
        nom = "Brochettes de viande",
        prix = 3000.00,
        image = drawableUri("brochette_poulet"),
        categoryId = CATEGORY_BROCHETTES_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Brochettes de viande"
    ),

    Produit(
        nom = "Brochettes de poisson",
        prix = 3000.00,
        image = drawableUri("brochette_poulet"),
        categoryId = CATEGORY_BROCHETTES_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Brochettes de poisson"
    ),

    // ---------- COMME AU VILLAGE ----------
    Produit(
        nom = "Poisson grillé à l'odika",
        prix = 5000.00,
        image = drawableUri("poisson_odika"),
        categoryId = CATEGORY_VILLAGE_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Plat du jour avec accompagnement"
    ),

    Produit(
        nom = "Poisson salé aux choux + carottes",
        prix = 5000.00,
        image = drawableUri("poisson_sale_choux"),
        categoryId = CATEGORY_VILLAGE_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Plat du jour avec accompagnement"
    ),

    Produit(
        nom = "Queue de bœuf mijotée / à l'odika",
        prix = 5000.00,
        image = drawableUri("queue_boeuf"),
        categoryId = CATEGORY_VILLAGE_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Mijotée ou à l'odika, avec accompagnement"
    ),

    Produit(
        nom = "Bouillon de poisson frais",
        prix = 5000.00,
        image = drawableUri("bouillon_poisson"),
        categoryId = CATEGORY_VILLAGE_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Plat du jour avec accompagnement"
    ),

    Produit(
        nom = "Bouillon de viande fraîche",
        prix = 5000.00,
        image = drawableUri("bouillon_viande"),
        categoryId = CATEGORY_VILLAGE_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Plat du jour avec accompagnement"
    ),

    // ---------- ACCOMPAGNEMENTS ----------
    // Le menu liste deux fois "Riz BOMA" (500 et 1000) — nommés ici petite / grande portion, à vérifier
    Produit(
        nom = "Riz BOMA (petite portion)",
        prix = 500.00,
        image = drawableUri("riz"),
        categoryId = CATEGORY_ACCOMP_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Riz maison"
    ),

    Produit(
        nom = "Manioc",
        prix = 500.00,
        image = drawableUri("manioc"),
        categoryId = CATEGORY_ACCOMP_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Bâton de manioc"
    ),

    Produit(
        nom = "Riz BOMA (grande portion)",
        prix = 1000.00,
        image = drawableUri("riz"),
        categoryId = CATEGORY_ACCOMP_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Riz maison"
    ),

    Produit(
        nom = "Haricots verts",
        prix = 1000.00,
        image = drawableUri("haricots_verts"),
        categoryId = CATEGORY_ACCOMP_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Haricots verts"
    ),

    Produit(
        nom = "Alloco banane",
        prix = 1000.00,
        image = drawableUri("alloco"),
        categoryId = CATEGORY_ACCOMP_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Bananes plantain frites"
    ),

    Produit(
        nom = "Pommes de terre sautées",
        prix = 1000.00,
        image = drawableUri("pommes_sautees"),
        categoryId = CATEGORY_ACCOMP_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Pommes de terre sautées"
    ),

    // ---------- BOISSONS NON ALCOOLISÉES ----------
    Produit(
        nom = "Eau Andza 1,5L",
        prix = 1000.00,
        image = drawableUri("andza"),
        categoryId = CATEGORY_SOFTS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Eau minérale — 1,5L"
    ),

    Produit(
        nom = "Coca-Cola",
        prix = 1000.00,
        image = drawableUri("coca_cola"),
        categoryId = CATEGORY_SOFTS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Soda — 33cl"
    ),

    Produit(
        nom = "Fanta",
        prix = 1000.00,
        image = drawableUri("fanta"),
        categoryId = CATEGORY_SOFTS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Soda — 33cl"
    ),

    Produit(
        nom = "Djino",
        prix = 1000.00,
        image = drawableUri("djino"),
        categoryId = CATEGORY_SOFTS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Soda — 33cl"
    ),

    Produit(
        nom = "Sprite",
        prix = 1000.00,
        image = drawableUri("sprite"),
        categoryId = CATEGORY_SOFTS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Soda — 33cl"
    ),

    Produit(
        nom = "Orangina",
        prix = 1000.00,
        image = drawableUri("orangina"),
        categoryId = CATEGORY_SOFTS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Boisson gazeuse à l’orange — 33cl"
    ),

    Produit(
        nom = "Tonic Imperial",
        prix = 1000.00,
        image = drawableUri("tonice"),
        categoryId = CATEGORY_SOFTS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Tonic — 33cl"
    ),

    Produit(
        nom = "Jus de pomme",
        prix = 1000.00,
        image = drawableUri("jus_pomme"),
        categoryId = CATEGORY_SOFTS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Jus de pomme"
    ),

    Produit(
        nom = "Jus Ceres",
        prix = 1000.00,
        image = drawableUri("ceres"),
        categoryId = CATEGORY_SOFTS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Jus de fruits Ceres"
    ),

    Produit(
        nom = "Sirop (menthe, grenadine, fraise)",
        prix = 2000.00,
        image = drawableUri("sirop"),
        categoryId = CATEGORY_SOFTS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Menthe, grenadine ou fraise"
    ),

    Produit(
        nom = "Red Bull",
        prix = 2000.00,
        image = drawableUri("redbull"),
        categoryId = CATEGORY_SOFTS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Boisson énergisante — 33cl"
    ),

    // ---------- BIÈRES ----------
    Produit(
        nom = "Booster",
        prix = 1000.00,
        image = drawableUri("boostercola"),
        categoryId = CATEGORY_BIERES_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Zombie, whisky, monster, banana..."
    ),

    Produit(
        nom = "Regab",
        prix = 1000.00,
        image = drawableUri("regab"),
        categoryId = CATEGORY_BIERES_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Bière blonde — bouteille 75cl"
    ),

    Produit(
        nom = "Regab Canette",
        prix = 1000.00,
        image = drawableUri("regabcanette"),
        categoryId = CATEGORY_BIERES_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Bière blonde — canette 50cl"
    ),

    Produit(
        nom = "Guinness Canette",
        prix = 1000.00,
        image = drawableUri("guinesscanette"),
        categoryId = CATEGORY_BIERES_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Bière brune — canette 33cl"
    ),

    Produit(
        nom = "33 Export Canette",
        prix = 1000.00,
        image = drawableUri("exportcanette"),
        categoryId = CATEGORY_BIERES_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Bière blonde — canette 33cl"
    ),

    Produit(
        nom = "Castel Canette",
        prix = 1000.00,
        image = drawableUri("castel"),
        categoryId = CATEGORY_BIERES_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Bière locale — canette 33cl"
    ),

    Produit(
        nom = "Beaufort",
        prix = 1000.00,
        image = drawableUri("beaufort"),
        categoryId = CATEGORY_BIERES_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Bière blonde — bouteille 75cl"
    ),

    Produit(
        nom = "Sombrero",
        prix = 1000.00,
        image = drawableUri("sombrero"),
        categoryId = CATEGORY_BIERES_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Bière tequila — bouteille 75cl"
    ),

    Produit(
        nom = "Moyenne Heineken",
        prix = 1500.00,
        image = drawableUri("heineken"),
        categoryId = CATEGORY_BIERES_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Bière blonde — format moyen"
    ),

    Produit(
        nom = "Grande Heineken",
        prix = 2000.00,
        image = drawableUri("heineken"),
        categoryId = CATEGORY_BIERES_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Bière blonde — grand format"
    ),

    Produit(
        nom = "Royal",
        prix = 1500.00,
        image = drawableUri("royal"),
        categoryId = CATEGORY_BIERES_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Bière blonde — 50cl"
    ),

    Produit(
        nom = "Smirnoff Ice",
        prix = 2000.00,
        image = drawableUri("smirnoff"),
        categoryId = CATEGORY_BIERES_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Boisson aromatisée — bouteille 33cl"
    ),

    Produit(
        nom = "Desperados",
        prix = 2000.00,
        image = drawableUri("despe"),
        categoryId = CATEGORY_BIERES_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Bière aromatisée — bouteille 33cl"
    ),

    // ---------- VINS ----------
    Produit(
        nom = "Petit Chenet",
        prix = 3000.00,
        image = drawableUri("chenet"),
        categoryId = CATEGORY_VINS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Vin rouge / blanc — 25cl"
    ),

    Produit(
        nom = "Vin rouge / blanc moelleux 75cl",
        prix = 7000.00,
        image = drawableUri("redwine"),
        categoryId = CATEGORY_VINS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Vin rouge ou blanc moelleux — à partir de 7000"
    ),

    // ---------- LIQUEURS ----------
    Produit(
        nom = "Campari (dose)",
        prix = 1000.00,
        image = drawableUri("campari"),
        categoryId = CATEGORY_LIQUEURS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Liqueur — à la dose"
    ),

    Produit(
        nom = "Campari (bouteille)",
        prix = 15000.00,
        image = drawableUri("campari"),
        categoryId = CATEGORY_LIQUEURS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Liqueur — bouteille 75cl"
    ),

    Produit(
        nom = "Martini (dose)",
        prix = 1000.00,
        image = drawableUri("martini"),
        categoryId = CATEGORY_LIQUEURS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Martini rouge ou blanc — à la dose"
    ),

    Produit(
        nom = "Martini (bouteille)",
        prix = 15000.00,
        image = drawableUri("martini"),
        categoryId = CATEGORY_LIQUEURS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Martini rouge ou blanc — bouteille 75cl"
    ),

    Produit(
        nom = "Suze (dose)",
        prix = 1000.00,
        image = drawableUri("suze"),
        categoryId = CATEGORY_LIQUEURS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Liqueur — à la dose"
    ),

    Produit(
        nom = "Suze (bouteille)",
        prix = 15000.00,
        image = drawableUri("suze"),
        categoryId = CATEGORY_LIQUEURS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Liqueur — bouteille 75cl"
    ),

    Produit(
        nom = "Black & White (dose)",
        prix = 1500.00,
        image = drawableUri("black_white"),
        categoryId = CATEGORY_LIQUEURS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Whisky — à la dose"
    ),

    Produit(
        nom = "Black & White (bouteille)",
        prix = 20000.00,
        image = drawableUri("black_white"),
        categoryId = CATEGORY_LIQUEURS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Whisky — bouteille 75cl"
    ),

    Produit(
        nom = "White Horse (dose)",
        prix = 1500.00,
        image = drawableUri("white_horse"),
        categoryId = CATEGORY_LIQUEURS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Whisky — à la dose"
    ),

    Produit(
        nom = "White Horse (bouteille)",
        prix = 20000.00,
        image = drawableUri("white_horse"),
        categoryId = CATEGORY_LIQUEURS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Whisky — bouteille 75cl"
    ),

    Produit(
        nom = "J&B (dose)",
        prix = 1500.00,
        image = drawableUri("jb"),
        categoryId = CATEGORY_LIQUEURS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Whisky — à la dose"
    ),

    Produit(
        nom = "J&B (bouteille)",
        prix = 20000.00,
        image = drawableUri("jb"),
        categoryId = CATEGORY_LIQUEURS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Whisky — bouteille 75cl"
    ),

    Produit(
        nom = "Jack Daniel's (dose)",
        prix = 2000.00,
        image = drawableUri("jack_daniels"),
        categoryId = CATEGORY_LIQUEURS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Whiskey — à la dose"
    ),

    Produit(
        nom = "Jack Daniel's (bouteille)",
        prix = 25000.00,
        image = drawableUri("jack_daniels"),
        categoryId = CATEGORY_LIQUEURS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Whiskey — bouteille 75cl"
    ),

    Produit(
        nom = "Bailey's (dose)",
        prix = 2000.00,
        image = drawableUri("baileys"),
        categoryId = CATEGORY_LIQUEURS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Crème de whisky — à la dose"
    ),

    Produit(
        nom = "Bailey's (bouteille)",
        prix = 25000.00,
        image = drawableUri("baileys"),
        categoryId = CATEGORY_LIQUEURS_BOMA_ID,
        stock = STOCK_INITIAL,
        description = "Crème de whisky — bouteille 75cl"
    )
)

val sampleVendeursBoma = listOf(
    Vendeur(
        nom = "Vendeur",
        prenom = "Boma"
    )
)