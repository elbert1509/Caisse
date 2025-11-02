package com.example.caisse.data

import com.example.caisse.R
import java.util.UUID


val CATEGORY_FOOD_ID = UUID.fromString("11111111-1111-1111-1111-111111111111")
val CATEGORY_DRINKS_ID = UUID.fromString("22222222-2222-2222-2222-222222222222")
val CATEGORY_DESSERTS_ID = UUID.fromString("33333333-3333-3333-3333-333333333333")
val CATEGORY_APPETIZERS_ID  = UUID.fromString("44444444-4444-4444-4444-444444444444")

val sampleCategories = listOf(
    Category(id = CATEGORY_FOOD_ID, name = "Food", description = "Repas et plats principaux"),
    Category(id = CATEGORY_DRINKS_ID, name = "Drinks", description = "Boissons"),
    Category(id = CATEGORY_DESSERTS_ID, name = "Desserts", description = "Pâtisseries et douceurs"),
    Category(id = CATEGORY_APPETIZERS_ID , name = "Appetizers", description = "Entrées")
)
val sampleProducts = listOf(
    // --- Food (15 produits) ---
    Produit(nom = "Burger", prix = 8.00, image = R.drawable.burger, categoryId = CATEGORY_FOOD_ID, stock = 20, description = "Burger maison avec fromage et salade"),
    Produit(nom = "Pizza Margherita", prix = 12.50, categoryId = CATEGORY_FOOD_ID, stock = 15, description = "Pizza classique à la tomate et mozzarella"),
    Produit(nom = "Salad", prix = 7.00, categoryId = CATEGORY_FOOD_ID, stock = 10, description = "Salade fraîche aux légumes de saison"),
    Produit(nom = "Steak", prix = 18.00, categoryId = CATEGORY_FOOD_ID, stock = 12, description = "Steak grillé accompagné de sauce"),
    Produit(nom = "Spaghetti Bolognese", prix = 14.00, categoryId = CATEGORY_FOOD_ID, stock = 18, description = "Pâtes italiennes à la sauce bolognaise"),
    Produit(nom = "Chicken Wings", prix = 9.50, categoryId = CATEGORY_FOOD_ID, stock = 25, description = "Ailes de poulet croustillantes"),
    Produit(nom = "Sushi Set", prix = 20.00, categoryId = CATEGORY_FOOD_ID, stock = 10, description = "Assortiment de sushis variés"),
    Produit(nom = "Tacos", prix = 11.00, categoryId = CATEGORY_FOOD_ID, stock = 20, description = "Tacos mexicains garnis de viande"),
    Produit(nom = "Lasagna", prix = 13.00, categoryId = CATEGORY_FOOD_ID, stock = 15, description = "Lasagnes italiennes au four"),
    Produit(nom = "Grilled Salmon", prix = 17.50, categoryId = CATEGORY_FOOD_ID, stock = 8, description = "Saumon grillé avec légumes"),
    Produit(nom = "Hot Dog", prix = 6.00, categoryId = CATEGORY_FOOD_ID, stock = 30, description = "Hot dog classique avec ketchup et moutarde"),
    Produit(nom = "Ramen", prix = 12.00, categoryId = CATEGORY_FOOD_ID, stock = 14, description = "Nouilles japonaises au bouillon"),
    Produit(nom = "Couscous", prix = 15.00, categoryId = CATEGORY_FOOD_ID, stock = 10, description = "Couscous marocain aux légumes et viande"),
    Produit(nom = "Falafel Wrap", prix = 9.00, categoryId = CATEGORY_FOOD_ID, stock = 22, description = "Wrap végétarien aux falafels"),
    Produit(nom = "Quiche Lorraine", prix = 8.50, categoryId = CATEGORY_FOOD_ID, stock = 12, description = "Quiche traditionnelle lorraine"),

    // --- Drinks (15 produits) ---
    Produit(nom = "Coca-Cola", prix = 2.50, categoryId = CATEGORY_DRINKS_ID, stock = 50, description = "Boisson gazeuse rafraîchissante"),
    Produit(nom = "Water", prix = 1.50, categoryId = CATEGORY_DRINKS_ID, stock = 100, description = "Eau minérale en bouteille"),
    Produit(nom = "Orange Juice", prix = 3.00, categoryId = CATEGORY_DRINKS_ID, stock = 40, description = "Jus d’orange pressé"),
    Produit(nom = "Apple Juice", prix = 3.00, categoryId = CATEGORY_DRINKS_ID, stock = 35, description = "Jus de pommes frais"),
    Produit(nom = "Lemonade", prix = 2.80, categoryId = CATEGORY_DRINKS_ID, stock = 45, description = "Limonade maison"),
    Produit(nom = "Iced Tea", prix = 3.20, categoryId = CATEGORY_DRINKS_ID, stock = 30, description = "Thé glacé au citron"),
    Produit(nom = "Espresso", prix = 2.00, categoryId = CATEGORY_DRINKS_ID, stock = 60, description = "Petit café serré"),
    Produit(nom = "Latte", prix = 3.50, categoryId = CATEGORY_DRINKS_ID, stock = 25, description = "Café au lait doux"),
    Produit(nom = "Cappuccino", prix = 3.80, categoryId = CATEGORY_DRINKS_ID, stock = 25, description = "Café mousseux italien"),
    Produit(nom = "Beer", prix = 5.00, categoryId = CATEGORY_DRINKS_ID, stock = 40, description = "Bière blonde pression"),
    Produit(nom = "Red Wine", prix = 8.00, categoryId = CATEGORY_DRINKS_ID, stock = 20, description = "Vin rouge de Bordeaux"),
    Produit(nom = "White Wine", prix = 8.00, categoryId = CATEGORY_DRINKS_ID, stock = 18, description = "Vin blanc de Bourgogne"),
    Produit(nom = "Mojito", prix = 7.50, categoryId = CATEGORY_DRINKS_ID, stock = 15, description = "Cocktail au rhum et menthe"),
    Produit(nom = "Smoothie", prix = 4.50, categoryId = CATEGORY_DRINKS_ID, stock = 28, description = "Smoothie aux fruits frais"),
    Produit(nom = "Milkshake", prix = 5.00, categoryId = CATEGORY_DRINKS_ID, stock = 22, description = "Milkshake à la vanille"),

    // --- Desserts (10 produits) ---
    Produit(nom = "Ice Cream", prix = 4.00, categoryId = CATEGORY_DESSERTS_ID, stock = 30, description = "Glace à la vanille artisanale"),
    Produit(nom = "Cake", prix = 5.00, categoryId = CATEGORY_DESSERTS_ID, stock = 25, description = "Part de gâteau au chocolat"),
    Produit(nom = "Donut", prix = 3.50, categoryId = CATEGORY_DESSERTS_ID, stock = 40, description = "Donut glacé au sucre"),
    Produit(nom = "Brownie", prix = 3.80, categoryId = CATEGORY_DESSERTS_ID, stock = 35, description = "Brownie au chocolat fondant"),
    Produit(nom = "Cheesecake", prix = 5.50, categoryId = CATEGORY_DESSERTS_ID, stock = 20, description = "Cheesecake new-yorkais"),
    Produit(nom = "Macarons", prix = 6.00, categoryId = CATEGORY_DESSERTS_ID, stock = 15, description = "Macarons colorés variés"),
    Produit(nom = "Cupcake", prix = 4.20, categoryId = CATEGORY_DESSERTS_ID, stock = 25, description = "Cupcake à la crème"),
    Produit(nom = "Pancakes", prix = 6.50, categoryId = CATEGORY_DESSERTS_ID, stock = 18, description = "Pancakes au sirop d’érable"),
    Produit(nom = "Waffles", prix = 6.00, categoryId = CATEGORY_DESSERTS_ID, stock = 20, description = "Gaufres croustillantes"),
    Produit(nom = "Mousse au chocolat", prix = 4.80, categoryId = CATEGORY_DESSERTS_ID, stock = 22, description = "Mousse légère au chocolat"),

    // --- Appetizers (10 produits) ---
    Produit(nom = "French Fries", prix = 6.00, categoryId = CATEGORY_APPETIZERS_ID, stock = 35, description = "Frites dorées et croustillantes"),
    Produit(nom = "Onion Rings", prix = 5.00, categoryId = CATEGORY_APPETIZERS_ID, stock = 28, description = "Beignets d’oignons frits"),
    Produit(nom = "Garlic Bread", prix = 4.50, categoryId = CATEGORY_APPETIZERS_ID, stock = 20, description = "Pain à l’ail grillé"),
    Produit(nom = "Spring Rolls", prix = 6.00, categoryId = CATEGORY_APPETIZERS_ID, stock = 25, description = "Rouleaux de printemps vietnamiens"),
    Produit(nom = "Mozzarella Sticks", prix = 6.50, categoryId = CATEGORY_APPETIZERS_ID, stock = 22, description = "Bâtonnets de mozzarella panés"),
    Produit(nom = "Chicken Nuggets", prix = 7.00, categoryId = CATEGORY_APPETIZERS_ID, stock = 30, description = "Nuggets de poulet frits"),
    Produit(nom = "Bruschetta", prix = 5.50, categoryId = CATEGORY_APPETIZERS_ID, stock = 18, description = "Tartines à la tomate et basilic"),
    Produit(nom = "Stuffed Mushrooms", prix = 7.50, categoryId = CATEGORY_APPETIZERS_ID, stock = 15, description = "Champignons farcis au fromage"),
    Produit(nom = "Nachos", prix = 8.00, categoryId = CATEGORY_APPETIZERS_ID, stock = 20, description = "Nachos au fromage et guacamole"),
    Produit(nom = "Shrimp Cocktail", prix = 9.00, categoryId = CATEGORY_APPETIZERS_ID, stock = 12, description = "Cocktail de crevettes")
)

val sampleVendeurs = listOf(
    Vendeur(id = 1, nom = "John", prenom = "Doe"),
    Vendeur(id = 2, nom = "Jane", prenom = "Smith"),
    Vendeur(id = 3, nom = "Bob", prenom = "Johnson")
)