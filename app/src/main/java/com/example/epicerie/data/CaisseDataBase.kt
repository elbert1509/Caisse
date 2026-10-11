package com.example.epicerie.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.epicerie.model.CategorieDao
import com.example.epicerie.model.ClotureDao
import com.example.epicerie.model.InfosDao
import com.example.epicerie.model.InvoiceDao
import com.example.epicerie.model.LogDao
import com.example.epicerie.model.ProduitDao
import com.example.epicerie.model.RecetteDao
import com.example.epicerie.model.TableDao
import com.example.epicerie.model.VendeurDao
import com.example.epicerie.model.VenteDao
import com.example.epicerie.model.VoitureDao

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
        Recette::class,
        LogTechnique::class,
        Cloture::class,
        EtatCaisse::class
               ],
    version = 11,
    // NF525 préconise exportSchema=true pour la traçabilité, mais Room 2.8.1 a un bug connu
    // d'incompatibilité kotlinx-serialization avec ce projet (KSP plante en désérialisant le
    // schéma JSON — AbstractMethodError sur FieldBundle$$serializer, indépendant du code ici).
    // v2_prod_v16 utilise Room 2.6.1 qui n'a pas ce problème. À réactiver si Room est mis à jour
    // ou downgradé, ou si le bug est corrigé en amont.
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
                            ).addMigrations(*ALL_MIGRATIONS).build()
                INSTANCE = instance
                instance
            }
        }
    }




}