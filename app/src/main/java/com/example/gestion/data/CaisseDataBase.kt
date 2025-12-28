package com.example.gestion.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.gestion.model.CategorieDao
import com.example.gestion.model.InfosDao
import com.example.gestion.model.InvoiceDao
import com.example.gestion.model.ProduitDao
import com.example.gestion.model.TableDao
import com.example.gestion.model.VendeurDao
import com.example.gestion.model.VenteDao

@Database(
    entities = [
        Category::class,
        Produit::class,
        Vendeur::class,
        Vente::class,
        VenteLigne::class,
        AppTable :: class,
        TableItem::class,
        Invoice::class,
        InvoiceItem::class,
        ShopInfos::class
               ],
    version = 7
    ,
    exportSchema = false
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