package com.example.caisse.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.caisse.model.CategorieDao
import com.example.caisse.model.ProduitDao
import com.example.caisse.model.VendeurDao
import com.example.caisse.model.VenteDao

@Database(
    entities = [Category::class, Produit::class, Vendeur::class,Vente::class, VenteLigne::class],
    version = 2,
    exportSchema = false
)



@TypeConverters(UUIDConverters::class)
abstract class CaisseDataBase : RoomDatabase() {
    abstract fun categorieDao(): CategorieDao
    abstract fun produitDao(): ProduitDao
    abstract fun vendeurDao(): VendeurDao
    abstract fun venteDao(): VenteDao


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