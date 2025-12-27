package com.example.oudeika.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.oudeika.model.CategorieDao
import com.example.oudeika.model.InfosDao
import com.example.oudeika.model.InvoiceDao
import com.example.oudeika.model.ProduitDao
import com.example.oudeika.model.TableDao
import com.example.oudeika.model.VendeurDao
import com.example.oudeika.model.VenteDao

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
    version = 8
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