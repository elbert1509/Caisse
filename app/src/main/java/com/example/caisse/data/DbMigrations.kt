package com.example.caisse.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

/**
 * Conversion des anciens identifiants auto-incrémentés (Int/Long) en UUID.
 *
 * Les ids auto-incrémentés entrent en collision entre appareils : deux pads qui créent chacun
 * un vendeur hors-ligne obtiennent tous deux l'id 2, et le second écrase le premier dans le
 * cloud (même problème pour les logs JET, avec perte de logs à la clé).
 *
 * Les UUID dérivés ici sont DÉTERMINISTES (UUID v3 à partir de l'ancien id ou du contenu) :
 * chaque appareil convertit la même donnée vers le même UUID, donc les données déjà
 * synchronisées convergent au lieu de se dupliquer. Le SyncWorker applique le même mapping
 * aux anciens documents Firestore (ids numériques) pour assurer la continuité.
 */
object LegacyIds {
    fun vendeurUuid(legacyId: Int): UUID =
        UUID.nameUUIDFromBytes("caisse-vendeur-legacy-$legacyId".toByteArray())

    /**
     * Basé sur le CONTENU du log (et non l'id local) : deux appareils qui détiennent le même
     * log JET (répliqué par la sync) dérivent le même UUID, même si leurs ids locaux différaient.
     */
    fun logUuid(date: String, typeEvenement: String, empreinte: String): UUID =
        UUID.nameUUIDFromBytes("caisse-log-legacy|$date|$typeEvenement|$empreinte".toByteArray())
}

private fun migrateToUuidIds(db: SupportSQLiteDatabase) {
    // ---- vendeur : INTEGER auto-incrément -> TEXT (UUID déterministe) ----
    db.execSQL(
        "CREATE TABLE IF NOT EXISTS `vendeur_new` (`id` TEXT NOT NULL, `nom` TEXT NOT NULL, " +
            "`prenom` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isDirty` INTEGER NOT NULL, " +
            "`isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`))"
    )
    db.query("SELECT id, nom, prenom, updatedAt, isDirty, isDeleted FROM vendeur").use { c ->
        while (c.moveToNext()) {
            db.execSQL(
                "INSERT OR REPLACE INTO vendeur_new VALUES (?,?,?,?,?,?)",
                arrayOf<Any?>(
                    LegacyIds.vendeurUuid(c.getInt(0)).toString(),
                    c.getString(1), c.getString(2), c.getLong(3), c.getInt(4), c.getInt(5)
                )
            )
        }
    }
    db.execSQL("DROP TABLE vendeur")
    db.execSQL("ALTER TABLE vendeur_new RENAME TO vendeur")

    // ---- Vente.vendeurId : INTEGER -> TEXT (même mapping que vendeur) ----
    db.execSQL(
        "CREATE TABLE IF NOT EXISTS `Vente_new` (`id` TEXT NOT NULL, `date` INTEGER NOT NULL, " +
            "`vendeurId` TEXT, `total` REAL NOT NULL, `tableId` TEXT, " +
            "`sequenceNumber` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
            "`isDirty` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `hash` TEXT NOT NULL, " +
            "`previousHash` TEXT NOT NULL, PRIMARY KEY(`id`))"
    )
    db.query(
        "SELECT id, date, vendeurId, total, tableId, sequenceNumber, updatedAt, isDirty, " +
            "isDeleted, hash, previousHash FROM Vente"
    ).use { c ->
        while (c.moveToNext()) {
            val vendeurId =
                if (c.isNull(2)) null else LegacyIds.vendeurUuid(c.getInt(2)).toString()
            db.execSQL(
                "INSERT OR REPLACE INTO Vente_new VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                arrayOf<Any?>(
                    c.getString(0), c.getLong(1), vendeurId, c.getDouble(3),
                    if (c.isNull(4)) null else c.getString(4),
                    c.getLong(5), c.getLong(6), c.getInt(7), c.getInt(8),
                    c.getString(9), c.getString(10)
                )
            )
        }
    }
    db.execSQL("DROP TABLE Vente")
    db.execSQL("ALTER TABLE Vente_new RENAME TO Vente")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_Vente_vendeurId` ON `Vente` (`vendeurId`)")

    // ---- logs_techniques : INTEGER auto-incrément -> TEXT (UUID dérivé du contenu) ----
    db.execSQL(
        "CREATE TABLE IF NOT EXISTS `logs_new` (`id` TEXT NOT NULL, `date` TEXT NOT NULL, " +
            "`typeEvenement` TEXT NOT NULL, `description` TEXT NOT NULL, `idVendeur` TEXT, " +
            "`empreinte` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isDirty` INTEGER NOT NULL, " +
            "`isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`))"
    )
    db.query(
        "SELECT date, typeEvenement, description, idVendeur, empreinte, updatedAt, isDirty, " +
            "isDeleted FROM logs_techniques"
    ).use { c ->
        while (c.moveToNext()) {
            db.execSQL(
                "INSERT OR REPLACE INTO logs_new VALUES (?,?,?,?,?,?,?,?,?)",
                arrayOf<Any?>(
                    LegacyIds.logUuid(c.getString(0), c.getString(1), c.getString(4)).toString(),
                    c.getString(0), c.getString(1), c.getString(2),
                    if (c.isNull(3)) null else c.getString(3),
                    c.getString(4), c.getLong(5), c.getInt(6), c.getInt(7)
                )
            )
        }
    }
    db.execSQL("DROP TABLE logs_techniques")
    db.execSQL("ALTER TABLE logs_new RENAME TO logs_techniques")
}

// Les versions 10, 11 et 12 partagent exactement le même schéma (bumps sans changement de
// structure) : on enregistre la même migration depuis chacune pour couvrir tout appareil du
// parc, quelle que soit la version de base qu'il a réellement installée.
val MIGRATIONS_TO_13: Array<Migration> = arrayOf(
    object : Migration(10, 13) {
        override fun migrate(db: SupportSQLiteDatabase) = migrateToUuidIds(db)
    },
    object : Migration(11, 13) {
        override fun migrate(db: SupportSQLiteDatabase) = migrateToUuidIds(db)
    },
    object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) = migrateToUuidIds(db)
    },
)
