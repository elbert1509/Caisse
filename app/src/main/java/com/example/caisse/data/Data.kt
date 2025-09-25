package com.example.caisse.data


import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.caisse.R
import java.util.UUID


enum class HomeActionButton{
    PRENDRE_COMMANDE,
    HISTORIQUE_COMMANDES,
    PARTAGER_BOUTONS,
    EXPORTER,
    GERER_INVENTAIRE,
    GERER_PRODUITS,
    GERE_CATEGORIE
    }

@Immutable
data class HomeTileData(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val action: HomeActionButton
)


@Entity(tableName = "Category")
data class Category(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val name: String,
    val description: String? = null,
    val icon: Int? = null
)

@Entity(
    tableName = "Produit",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class Produit(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val nom: String,
    val prix: Double,
    val image: Int? = null,
    val categoryId: UUID,   // 🔗 clé étrangère
    val stock: Int = 0,
    val description: String? = null,
    val isActive: Boolean = true
)

@Entity(tableName = "vendeur")
data class Vendeur(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nom: String,
    val prenom: String,
)

data class Ticket(
    val produit: Produit,
    var quantity: Int
)


val sampleCategories = listOf(
    Category(id = UUID.randomUUID(), name = "Food", description = "Repas et plats principaux"),
    Category(id = UUID.randomUUID(), name = "Drinks", description = "Boissons"),
    Category(id = UUID.randomUUID(), name = "Desserts", description = "Pâtisseries et douceurs")
)

val sampleProducts = listOf(
    Produit(
        id = UUID.randomUUID(),
        nom = "Burger",
        prix = 8.00,
        image = R.drawable.burger,
        categoryId = sampleCategories[0].id,
        stock = 20,
        description = "Un délicieux burger maison"
    ),
    Produit(
        id = UUID.randomUUID(),
        nom = "Pizza",
        prix = 12.50,
        image = 0,
        categoryId = sampleCategories[0].id,
        stock = 15,
        description = "Pizza Margherita traditionnelle"
    ),
    Produit(
        id = UUID.randomUUID(),
        nom = "Salad",
        prix = 7.00,
        image = 0,
        categoryId = sampleCategories[0].id,
        stock = 10
    ),
    Produit(
        id = UUID.randomUUID(),
        nom = "Coca-Cola",
        prix = 2.50,
        image = 0,
        categoryId = sampleCategories[1].id,
        stock = 50
    ),
    Produit(
        id = UUID.randomUUID(),
        nom = "Water",
        prix = 1.50,
        image = 0,
        categoryId = sampleCategories[1].id,
        stock = 100
    ),
    Produit(
        id = UUID.randomUUID(),
        nom = "Ice Cream",
        prix = 4.00,
        image = 0,
        categoryId = sampleCategories[2].id,
        stock = 30
    ),
    Produit(
        id = UUID.randomUUID(),
        nom = "Cake",
        prix = 5.00,
        image = 0,
        categoryId = sampleCategories[2].id,
        stock = 25
    )
)