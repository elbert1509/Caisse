package com.example.caisse.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
    version = 10,
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

        fun getDatabase(context: Context): CaisseDataBase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CaisseDataBase::class.java,
                    "caisse_database"
                ).fallbackToDestructiveMigration(false).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
