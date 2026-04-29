package com.example.caisse.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.caisse.model.CategorieDao
import com.example.caisse.model.ClotureDao
import com.example.caisse.model.InfosDao
import com.example.caisse.model.InvoiceDao
import com.example.caisse.model.LogDao
import com.example.caisse.model.ProduitDao
import com.example.caisse.model.TableDao
import com.example.caisse.model.VendeurDao
import com.example.caisse.model.VenteDao

@Database(
    entities = [
        Category::class,
        Produit::class,
        Vendeur::class,
        Vente::class,
        VenteLigne::class,
        AppTable::class,
        TableItem::class,
        Invoice::class,
        InvoiceItem::class,
        ShopInfos::class,
        LogTechnique::class,
        Cloture::class,
        EtatCaisse::class
    ],
    version = 11,
    exportSchema = true   // NF525 : traçabilité des évolutions du schéma
)
@TypeConverters(UUIDConverters::class)
abstract class CaisseDataBase : RoomDatabase() {
    abstract fun categorieDao(): CategorieDao
    abstract fun produitDao(): ProduitDao
    abstract fun vendeurDao(): VendeurDao
    abstract fun venteDao(): VenteDao
    abstract fun tableDao(): TableDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun infosDao(): InfosDao
    abstract fun logDao(): LogDao
    abstract fun clotureDao(): ClotureDao

    companion object {
        @Volatile
        private var INSTANCE: CaisseDataBase? = null

        /**
         * NF525 Axe C — Migration explicite, pas de fallbackToDestructiveMigration.
         * En cas d'échec de migration, l'app lève une exception plutôt que de détruire les données.
         */
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ajout du numéro de séquence ininterrompu (NF525 Axe B)
                db.execSQL("ALTER TABLE Vente ADD COLUMN sequenceNumber INTEGER NOT NULL DEFAULT 0")
                // Taux de TVA par produit pour ventilation correcte dans les rapports
                db.execSQL("ALTER TABLE Produit ADD COLUMN tauxTVA REAL NOT NULL DEFAULT 20.0")
                db.execSQL("ALTER TABLE VenteLigne ADD COLUMN tauxTVA REAL NOT NULL DEFAULT 20.0")
            }
        }

        fun getMigrations() = arrayOf(MIGRATION_10_11)

        fun getDatabase(context: Context): CaisseDataBase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CaisseDataBase::class.java,
                    "caisse_database"
                )
                    .addMigrations(MIGRATION_10_11)
                    // NF525 Axe C : aucune migration destructive autorisée
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
