package com.example.caisse.data


import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
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
    DONNES,
    DASHBOARD,
    STOCK,
    GESTION,
    }

@Immutable
data class HomeTileData(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val action: HomeActionButton
)


@Entity(tableName = "category")
data class Category(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val name: String,
    val description: String? = null,
    val icon: Int? = null,
    // sync
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
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
    val image: String? = null,
    val categoryId: UUID,   // 🔗 clé étrangère
    val stock: Int = 0,
    val description: String? = null,
    val isActive: Boolean = true,
    val tauxTVA: Double = 20.0,
    // sync
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(tableName = "vendeur")
data class Vendeur(
    // UUID (et pas auto-incrément) : deux appareils qui créent chacun un vendeur hors-ligne
    // ne doivent pas entrer en collision sur le même id lors de la synchronisation.
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val nom: String,
    val prenom: String,
    // PIN à 4 chiffres défini par le gérant pour la connexion du vendeur (vide = pas encore défini)
    val pinHash: String = "",
    val pinSalt: String = "",
    // sync
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)


@Entity(
    tableName = "Vente",
    indices = [Index("vendeurId")]
)
data class Vente(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val date: Long = System.currentTimeMillis(),
    val vendeurId: UUID? = null,
    val total: Double,
    val tableId: UUID? =  UUID.fromString("22222222-0000-2222-2222-222222222222"),
    val sequenceNumber: Long = 0,
    // sync
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false,
    val hash: String = "",
    val previousHash: String = ""
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
    val sousTotal: Double,
    val tauxTVA: Double = 20.0,
    // sync
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)


data class Ticket(
    val produit: Produit,
    var quantity: Int
)
data class VenteLigneWithProduit(
    @Embedded val ligne: VenteLigne,

    @Relation(
        parentColumn = "produitId",
        entityColumn = "id"
    )
    val produit: Produit
)

data class VenteWithDetails(
    @Embedded val vente: Vente,

    // NF525 : les lignes supprimées ne sont jamais effacées physiquement (traçabilité), donc
    // cette relation Room (qui ne filtre pas isDeleted) peut renvoyer à la fois l'ancienne ligne
    // supprimée et sa remplaçante après une correction de panier — d'où lignesActives ci-dessous.
    @Relation(
        entity = VenteLigne::class,
        parentColumn = "id",
        entityColumn = "venteId"
    )
    val lignes: List<VenteLigneWithProduit>
) {
    val lignesActives: List<VenteLigneWithProduit>
        get() = lignes.filter { !it.ligne.isDeleted }
}

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSignedIn: Boolean = false
)

@Entity(tableName = "invoice")
data class Invoice(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val tableId: UUID,
    val totalAmount: Double,
    val date: Long = System.currentTimeMillis(),
    // sync
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
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
    val quantity: Int,
    // sync
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)

// Rapport produit pour une période donnée (Jour / Semaine / Mois)
data class ProductReport(
    val productName: String,
    val totalQuantity: Int,
    val productStock: Int,
    val revenue: Double
)

@Entity(tableName = "ShopInfos")
data class ShopInfos (
    @PrimaryKey val id: Int = 1,
    val name: String,
    val address: String,
    val phone: String,
    val email: String,
    val siret: String,
    val logo: Int? = null,
    val passwordHash: String,
    val passwordSalt: String,
    val devise : String,
    // Heure (0-23) à laquelle une caisse vendeur encore ouverte est fermée automatiquement.
    val heureClotureAuto: Int = 5,
    // sync
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(tableName = "app_table")
data class AppTable(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val name: String,
    var active: Boolean = true,
    // Vendeur propriétaire de la table (celui qui l'a créée ou qui l'a prise en premier)
    val vendeurId: UUID? = null,
    // sync
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
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
    val quantity: Int,
    // sync
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(tableName = "logs_techniques")
data class LogTechnique(
    // UUID (et pas auto-incrément) : les ids locaux entraient en collision entre appareils,
    // ce qui écrasait/faisait sauter des logs JET lors de la synchronisation (NF525).
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val date: String, // Format ISO8601
    val typeEvenement: String, // ex: "OUVERTURE_SESSION", "ERREUR_SYSTEME", "MODIF_PRIX"
    val description: String,
    val idVendeur: UUID?,
    val empreinte: String, // Hash pour prouver que le log n'a pas été modifié
    // sync
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)
enum class TypeEvenement {
    OUVERTURE_SESSION,
    FERMETURE_SESSION,
    CLOTURE_JOURNALIERE,
    CLOTURE_ET_FERMETURE,
    ERREUR_SYSTEME,
    ERREUR_CLOTURE,
    MODIF_PRIX,
    RESET_CATALOGUE,
    IMPRESSION_TICKET,
    IMPRESSION_ANNULEE,
    VENTE_ANNULEE,
    SYNC_CLOUD,
    VENTE_VALIDEE,
    AJOUT_PRODUIT,
    MODIF_PRODUIT,
    SUPPRESSION_PRODUIT,
    AJOUT_CATEGORIE,
    MODIF_CATEGORIE,
    SUPPRESSION_CATEGORIE,
    AJOUT_VENDEUR,
    SUPPRESSION_VENDEUR,
    MODIF_CONFIG,
    MODIF_MOT_DE_PASSE,
    OUVERTURE_TABLE,
    FERMETURE_TABLE
}

@Entity(tableName = "clotures",
        indices = [Index(value = ["dateCloture", "type"], unique = true)]
)
data class Cloture(
    @PrimaryKey val idCloture: UUID = UUID.randomUUID(),
    val dateCloture: String, // Date du jour
    val type: String, // "JOURNALIERE", "MENSUELLE"
    val chiffreAffaireBrut: Double,
    val totalTVA: Double,
    val compteurVentes: Int, // Nombre de tickets
    val grandTotalCumule: Double, // Somme ininterrompue depuis le début de l'app
    val hash: String, // Signature de la clôture
    // sync
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)
object AppConfig {
    const val VERSION_LOGICIEL = "1.3"
    const val NOM_LOGICIEL = "MaCaissePro"
    const val EDITEUR = "Ogooué Infos"
    const val NUMERO = "0667724958"
    const val NUM_CERTIFICAT = "NF525-EN COURS" // À remplacer par le numéro fourni par l'organisme certificateur
}

@Entity(tableName = "etat_caisse")
data class EtatCaisse(
    @PrimaryKey val id: Int = 1, // Une seule ligne possible
    val isOuverte: Boolean = false,
    val dateOuverture: String? = null,
    val idVendeurOuverture: UUID? = null
)

/**
 * Historique des ouvertures/fermetures de caisse : permet de rattacher une vente au
 * "jour métier" (jourMetier = date d'ouverture) plutôt qu'au jour calendaire, pour que les
 * ventes réalisées après minuit tant que la caisse de la veille n'est pas fermée comptent
 * pour la veille.
 */
@Entity(tableName = "session_caisse")
data class SessionCaisse(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val dateOuverture: Long, // epoch millis
    val dateFermeture: Long? = null, // null tant que la session est ouverte
    val jourMetier: String, // LocalDate.toString() du jour d'ouverture
    val idVendeurOuverture: UUID? = null,
    // sync
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = true
)

data class SalesData(val label: String, val amount: Double)
data class ProductSale(val productName: String, val totalQuantity: Int)

// Vue live du gérant : statut de la caisse de chaque vendeur + ses ventes en session.
data class VendeurSuiviUi(
    val vendeurId: UUID,
    val nom: String,
    val ouverte: Boolean,
    val heureOuverture: Long?,
    val heureFermeture: Long?,
    val totalVentesSession: Double
)

