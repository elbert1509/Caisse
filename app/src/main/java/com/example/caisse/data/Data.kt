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
    GERE_CATEGORIE,
    TABLE,
    DONNES
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


@Entity(
    tableName = "Vente",
    foreignKeys = [
        ForeignKey(
            entity = Vendeur::class,
            parentColumns = ["id"],
            childColumns = ["vendeurId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("vendeurId")]
)
data class Vente(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val date: Long = System.currentTimeMillis(),
    val vendeurId: Int? = null,
    val total: Double
)

@Entity(
    tableName = "VenteLigne",
    foreignKeys = [
        ForeignKey(
            entity = Vente::class,
            parentColumns = ["id"],
            childColumns = ["venteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Produit::class,
            parentColumns = ["id"],
            childColumns = ["produitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("venteId"), Index("produitId")]
)
data class VenteLigne(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val venteId: UUID,
    val produitId: UUID,
    val quantity: Int,
    val prixUnitaire: Double,
    val sousTotal: Double
)


data class Ticket(
    val produit: Produit,
    var quantity: Int
)
data class VenteWithDetails(
    val vente: Vente,
    val lignes: List<Ticket>
)

@Entity(tableName = "invoice")
data class Invoice(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val tableId: UUID,
    val totalAmount: Double,
    val date: Long = System.currentTimeMillis()
)
@Entity(
    tableName = "invoice_item",
    foreignKeys = [
        ForeignKey(
            entity = Invoice::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Produit::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class InvoiceItem(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val invoiceId: UUID,
    val productId: UUID,
    val quantity: Int
)


@Entity(tableName = "app_table")
data class AppTable(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val name: String,
    var active: Boolean = true
)

@Entity(
    tableName = "table_item",
    foreignKeys = [
        ForeignKey(
            entity = AppTable::class,
            parentColumns = ["id"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Produit::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TableItem(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val tableId: UUID,
    val productId: UUID,
    val quantity: Int
)
