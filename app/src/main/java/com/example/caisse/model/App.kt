package com.example.caisse.model
import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        // Crashs et gels de l'interface -> journal des événements (JET).
        com.example.caisse.util.CrashJournal.install(this)
        cancelLegacySyncWork()
        schedulePeriodicSync()
        schedulePeriodicAutoCloture()
    }

    /**
     * Une seule fois après la mise à jour : annule les synchros ponctuelles créées par les
     * anciennes versions (sans contrainte réseau, non uniques). Hors connexion elles restaient
     * bloquées et s'accumulaient ; WorkManager les relançait même après un redémarrage.
     * La périodique, aussi taguée "sync", est annulée avec elles puis replanifiée juste après
     * (KEEP ré-enfile un travail annulé ; WorkManager exécute ces opérations dans l'ordre).
     * Les données non envoyées restent `isDirty` en base : rien n'est perdu.
     */
    private fun cancelLegacySyncWork() {
        val prefs = getSharedPreferences("sync", MODE_PRIVATE)
        if (prefs.getBoolean(LEGACY_SYNC_CANCELLED, false)) return
        WorkManager.getInstance(this).cancelAllWorkByTag("sync")
        prefs.edit().putBoolean(LEGACY_SYNC_CANCELLED, true).apply()
    }

    private companion object {
        const val LEGACY_SYNC_CANCELLED = "legacySyncWorkCancelled"
    }

    /**
     * Synchro automatique de fond toutes les 15 minutes (minimum imposé par WorkManager).
     * Sert de filet de sécurité : la fraîcheur "temps réel" vient surtout de la synchro
     * événementielle (création de table, vente, ouverture d'app, pull-to-refresh).
     * Ne s'exécute que si le réseau est disponible.
     */
    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag("sync")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "sync_periodic",
            ExistingPeriodicWorkPolicy.KEEP, // garde la planification existante au redémarrage
            request
        )
    }

    /**
     * Vérifie toutes les 15 minutes (minimum WorkManager) si l'heure de fermeture automatique
     * configurée par le gérant est dépassée et la caisse encore ouverte — ne nécessite pas le
     * réseau, c'est une opération purement locale.
     */
    private fun schedulePeriodicAutoCloture() {
        val request = PeriodicWorkRequestBuilder<AutoClotureWorker>(15, TimeUnit.MINUTES)
            .addTag("auto_cloture")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "auto_cloture_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
