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
        schedulePeriodicSync()
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
}
