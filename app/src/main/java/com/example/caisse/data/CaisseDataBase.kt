package com.example.piece.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.piece.model.CategorieDao
import com.example.piece.model.InfosDao
import com.example.piece.model.InvoiceDao
import com.example.piece.model.ProduitDao
import com.example.piece.model.RecetteDao
import com.example.piece.model.TableDao
import com.example.piece.model.VendeurDao
import com.example.piece.model.VenteDao
import com.example.piece.model.VoitureDao

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
        ShopInfos::class,
        Voiture::class,
        Recette::class
               ],
    version = 10
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
    abstract fun voitureDao(): VoitureDao
    abstract fun recetteDao(): RecetteDao




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