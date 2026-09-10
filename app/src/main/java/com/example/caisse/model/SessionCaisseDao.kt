package com.example.caisse.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.caisse.data.SessionCaisse
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface SessionCaisseDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: SessionCaisse)

    // Pull cloud : upsert (une session peut déjà exister localement avec le même id).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionCaisse)

    @Query("SELECT * FROM session_caisse WHERE id = :id")
    suspend fun getSessionById(id: UUID): SessionCaisse?

    // Ferme la session actuellement ouverte (il ne peut y en avoir qu'une à la fois).
    @Query("UPDATE session_caisse SET dateFermeture = :ts, updatedAt = :ts, isDirty = 1 WHERE dateFermeture IS NULL")
    suspend fun closeCurrent(ts: Long)

    // Ferme uniquement la session ouverte de CE vendeur (plusieurs vendeurs peuvent avoir une
    // caisse ouverte en même temps).
    @Query("UPDATE session_caisse SET dateFermeture = :ts, updatedAt = :ts, isDirty = 1 WHERE dateFermeture IS NULL AND idVendeurOuverture = :vendeurId")
    suspend fun closeCurrentForVendeur(vendeurId: UUID?, ts: Long)

    // Sync : sessions à pousser vers le cloud (nouvelles ou dont dateFermeture vient de changer).
    @Query("SELECT * FROM session_caisse WHERE isDirty = 1")
    suspend fun getDirtySessions(): List<SessionCaisse>

    @Query("UPDATE session_caisse SET isDirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun clearDirty(id: UUID, updatedAt: Long)

    @Query("SELECT * FROM session_caisse WHERE dateFermeture IS NULL LIMIT 1")
    suspend fun getCurrentSession(): SessionCaisse?

    @Query("SELECT * FROM session_caisse WHERE dateFermeture IS NULL AND idVendeurOuverture = :vendeurId LIMIT 1")
    suspend fun getCurrentSessionForVendeur(vendeurId: UUID?): SessionCaisse?

    @Query("SELECT EXISTS(SELECT 1 FROM session_caisse WHERE dateFermeture IS NULL AND idVendeurOuverture = :vendeurId)")
    fun observeSessionOuverte(vendeurId: UUID): Flow<Boolean>

    // Toutes les sessions actuellement ouvertes, tous vendeurs confondus (vue live du gérant).
    @Query("SELECT * FROM session_caisse WHERE dateFermeture IS NULL")
    fun getOpenSessionsFlow(): Flow<List<SessionCaisse>>

    // Variante suspend (hors Flow) : clôture automatique déclenchée depuis le SyncWorker.
    @Query("SELECT * FROM session_caisse WHERE dateFermeture IS NULL")
    suspend fun getOpenSessionsOnce(): List<SessionCaisse>

    // Clôture automatique : ferme UNE session précise à un instant donné (l'heure de clôture
    // configurée), plutôt qu'à l'instant du contrôle (qui ne tombe pas forcément pile dessus).
    @Query("UPDATE session_caisse SET dateFermeture = :ts, updatedAt = :ts, isDirty = 1 WHERE id = :id")
    suspend fun closeSessionAt(id: UUID, ts: Long)

    // Dernière session (ouverte ou fermée) d'un vendeur donné, pour afficher sa dernière
    // heure d'ouverture/fermeture même quand sa caisse est actuellement fermée.
    @Query("SELECT * FROM session_caisse WHERE idVendeurOuverture = :vendeurId ORDER BY dateOuverture DESC LIMIT 1")
    fun getLastSessionForVendeurFlow(vendeurId: UUID): Flow<SessionCaisse?>

    // Sessions ouvertes depuis :startDate, ou toujours en cours (pour couvrir une session
    // ouverte avant :startDate mais dont la fermeture n'a pas encore eu lieu).
    @Query("SELECT * FROM session_caisse WHERE dateOuverture >= :startDate OR dateFermeture IS NULL ORDER BY dateOuverture ASC")
    fun getSessionsSince(startDate: Long): Flow<List<SessionCaisse>>
}
