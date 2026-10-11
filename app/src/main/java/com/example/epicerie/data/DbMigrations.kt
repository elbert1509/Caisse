package com.example.epicerie.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

/**
 * Migrations explicites de la base Room locale.
 *
 * Volontairement pas de `fallbackToDestructiveMigration` (voir CaisseDataBase.kt) : un bump de
 * `version` sans migration correspondante ici doit faire planter l'app au démarrage (erreur
 * visible et bloquante en dev/test) plutôt que d'effacer silencieusement les données d'un
 * magasin en prod. Toute évolution de schéma doit ajouter sa Migration ici AVANT de bumper
 * `version` dans CaisseDataBase.kt.
 *
 * ⚠️ Cette migration (10 -> 11) n'a été validée que par compilation, pas par un test de
 * migration réel sur un appareil avec des données existantes (voir DEPLOIEMENT_PAD.md /
 * le plan de rebase). À tester explicitement : installer une build v10, créer des données,
 * installer cette build v11 PAR-DESSUS (sans désinstaller), vérifier qu'aucune donnée n'est
 * perdue et qu'il n'y a pas de crash au démarrage.
 */

/**
 * Conversion des anciens identifiants auto-incrémentés (Int) en UUID pour Vendeur/Vente.vendeurId.
 *
 * Les ids auto-incrémentés entrent en collision entre appareils : deux pads qui créent chacun
 * un vendeur hors-ligne obtiennent tous deux l'id 1, et le second écrase le premier dans le
 * cloud. Les UUID dérivés ici sont DÉTERMINISTES (dérivés de l'ancien id) : chaque appareil
 * convertit la même donnée vers le même UUID, donc les données déjà synchronisées convergent
 * au lieu de se dupliquer.
 */
object LegacyIds {
    fun vendeurUuid(legacyId: Int): UUID =
        UUID.nameUUIDFromBytes("epicerie-vendeur-legacy-$legacyId".toByteArray())
}

private fun migrateVendeurEtVenteVersUuid(db: SupportSQLiteDatabase) {
    // ---- vendeur : id INTEGER auto-incrément -> TEXT (UUID déterministe) ----
    db.execSQL(
        "CREATE TABLE IF NOT EXISTS `Vendeur_new` (`id` TEXT NOT NULL, `nom` TEXT NOT NULL, " +
            "`prenom` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isDirty` INTEGER NOT NULL, " +
            "`isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`))"
    )
    // Table d'ancien id -> nouvel UUID, utilisée ci-dessous pour réécrire Vente.vendeurId.
    val idMap = mutableMapOf<Int, String>()
    db.query("SELECT id, nom, prenom, updatedAt, isDirty, isDeleted FROM Vendeur").use { c ->
        while (c.moveToNext()) {
            val oldId = c.getInt(0)
            val newId = LegacyIds.vendeurUuid(oldId).toString()
            idMap[oldId] = newId
            db.execSQL(
                "INSERT OR REPLACE INTO Vendeur_new VALUES (?,?,?,?,?,?)",
                arrayOf<Any?>(newId, c.getString(1), c.getString(2), c.getLong(3), c.getInt(4), c.getInt(5))
            )
        }
    }
    db.execSQL("DROP TABLE Vendeur")
    db.execSQL("ALTER TABLE Vendeur_new RENAME TO Vendeur")

    // ---- Vente : vendeurId INTEGER -> TEXT (même mapping), + sequenceNumber/hash/previousHash ----
    db.execSQL(
        "CREATE TABLE IF NOT EXISTS `Vente_new` (`id` TEXT NOT NULL, `date` INTEGER NOT NULL, " +
            "`vendeurId` TEXT, `total` REAL NOT NULL, `tableId` TEXT, " +
            "`sequenceNumber` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
            "`isDirty` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `hash` TEXT NOT NULL, " +
            "`previousHash` TEXT NOT NULL, PRIMARY KEY(`id`))"
    )
    db.query(
        "SELECT id, date, vendeurId, total, tableId, updatedAt, isDirty, isDeleted FROM Vente"
    ).use { c ->
        while (c.moveToNext()) {
            val oldVendeurId = if (c.isNull(2)) null else c.getInt(2)
            val newVendeurId = oldVendeurId?.let { idMap[it] ?: LegacyIds.vendeurUuid(it).toString() }
            db.execSQL(
                "INSERT OR REPLACE INTO Vente_new " +
                    "(id, date, vendeurId, total, tableId, sequenceNumber, updatedAt, isDirty, isDeleted, hash, previousHash) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                arrayOf<Any?>(
                    c.getString(0), c.getLong(1), newVendeurId, c.getDouble(3),
                    if (c.isNull(4)) null else c.getString(4),
                    0L, c.getLong(5), c.getInt(6), c.getInt(7),
                    "", ""
                )
            )
        }
    }
    db.execSQL("DROP TABLE Vente")
    db.execSQL("ALTER TABLE Vente_new RENAME TO Vente")
}

private fun creerTablesNf525(db: SupportSQLiteDatabase) {
    db.execSQL(
        "CREATE TABLE IF NOT EXISTS `logs_techniques` (`id` TEXT NOT NULL, `date` TEXT NOT NULL, " +
            "`typeEvenement` TEXT NOT NULL, `description` TEXT NOT NULL, `idVendeur` TEXT, " +
            "`empreinte` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isDirty` INTEGER NOT NULL, " +
            "`isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`))"
    )
    db.execSQL(
        "CREATE TABLE IF NOT EXISTS `clotures` (`idCloture` TEXT NOT NULL, `dateCloture` TEXT NOT NULL, " +
            "`type` TEXT NOT NULL, `chiffreAffaireBrut` REAL NOT NULL, `totalTVA` REAL NOT NULL, " +
            "`compteurVentes` INTEGER NOT NULL, `grandTotalCumule` REAL NOT NULL, `hash` TEXT NOT NULL, " +
            "`updatedAt` INTEGER NOT NULL, `isDirty` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, " +
            "PRIMARY KEY(`idCloture`))"
    )
    db.execSQL(
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_clotures_dateCloture_type` ON `clotures` (`dateCloture`, `type`)"
    )
    db.execSQL(
        "CREATE TABLE IF NOT EXISTS `etat_caisse` (`id` INTEGER NOT NULL, `isOuverte` INTEGER NOT NULL, " +
            "`dateOuverture` TEXT, `idVendeurOuverture` TEXT, PRIMARY KEY(`id`))"
    )
}

private fun ajouterTauxTVA(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE VenteLigne ADD COLUMN tauxTVA REAL NOT NULL DEFAULT 20.0")
}

/**
 * Produit.image : INTEGER (ancien ID de ressource locale, ex. R.drawable.x) -> TEXT (URI).
 * SQLite ne sait pas convertir un ancien ID de ressource en URI sans connaître son nom — les
 * images existantes repassent à NULL (données de test uniquement à ce stade du projet) ;
 * l'utilisateur ré-associe l'image depuis l'écran produit ou via l'import de catalogue.
 * Recrée aussi la clé étrangère + les index (SQLite ne permet pas d'ALTER un type de colonne).
 */
private fun migrerProduitImageVersUri(db: SupportSQLiteDatabase) {
    db.execSQL(
        "CREATE TABLE IF NOT EXISTS `Produit_new` (`id` TEXT NOT NULL, `nom` TEXT NOT NULL, " +
            "`prix` REAL NOT NULL, `image` TEXT, `categoryId` TEXT NOT NULL, " +
            "`stock` INTEGER NOT NULL, `description` TEXT, `isActive` INTEGER NOT NULL, " +
            "`codeBarre` TEXT, `updatedAt` INTEGER NOT NULL, `isDirty` INTEGER NOT NULL, " +
            "`isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
            "FOREIGN KEY(`categoryId`) REFERENCES `category`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
    )
    db.query(
        "SELECT id, nom, prix, categoryId, stock, description, isActive, codeBarre, updatedAt, isDirty, isDeleted FROM Produit"
    ).use { c ->
        while (c.moveToNext()) {
            db.execSQL(
                "INSERT OR REPLACE INTO Produit_new " +
                    "(id, nom, prix, image, categoryId, stock, description, isActive, codeBarre, updatedAt, isDirty, isDeleted) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                arrayOf<Any?>(
                    c.getString(0), c.getString(1), c.getDouble(2), null, c.getString(3),
                    c.getInt(4), if (c.isNull(5)) null else c.getString(5), c.getInt(6),
                    if (c.isNull(7)) null else c.getString(7), c.getLong(8), c.getInt(9), c.getInt(10)
                )
            )
        }
    }
    db.execSQL("DROP TABLE Produit")
    db.execSQL("ALTER TABLE Produit_new RENAME TO Produit")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_Produit_categoryId` ON `Produit` (`categoryId`)")
    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_Produit_codeBarre` ON `Produit` (`codeBarre`)")
}

val MIGRATION_10_11: Migration = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        migrateVendeurEtVenteVersUuid(db)
        ajouterTauxTVA(db)
        migrerProduitImageVersUri(db)
        creerTablesNf525(db)
    }
}

val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_10_11)
