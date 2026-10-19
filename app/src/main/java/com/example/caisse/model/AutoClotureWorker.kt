package com.example.caisse.model

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.caisse.data.CaisseDataBase
import com.example.caisse.data.CaisseRepository
import java.util.Calendar

/**
 * Filet de sécurité NF525 : si le gérant oublie de clôturer la caisse, elle se ferme seule
 * à l'heure configurée dans Infos (champ `heureFermetureAuto`, "05:00" par défaut).
 *
 * Tourne au même rythme que la synchro (toutes les 15 min, minimum WorkManager) — la fermeture
 * se déclenche donc avec jusqu'à ~15 min de retard sur l'heure configurée, ce qui est
 * acceptable pour ce cas d'usage (pas besoin de précision à la minute).
 *
 * Pas de déclenchement en double : `genererClotureJournaliere()` refuse une 2e clôture pour
 * la même date (exception avalée ici), donc une fermeture manuelle plus tôt dans la journée,
 * ou une réouverture suivie d'une activité après l'heure configurée, ne provoque pas de
 * nouvelle clôture tant que le jour n'a pas changé.
 */
class AutoClotureWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        val db = CaisseDataBase.getDatabase(applicationContext)
        val repository = CaisseRepository(
            db.categorieDao(),
            db.produitDao(),
            db.vendeurDao(),
            db.venteDao(),
            db.tableDao(),
            db.invoiceDao(),
            db.infosDao(),
            db.logDao(),
            db.clotureDao(),
            db.mouvementStockDao()
        )

        return try {
            if (!repository.estCaisseOuverte()) return Result.success()

            val infos = repository.getInfos() ?: return Result.success()
            val parts = infos.heureFermetureAuto.split(":")
            val heure = parts.getOrNull(0)?.toIntOrNull() ?: return Result.success()
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

            val maintenant = Calendar.getInstance()
            val cible = (maintenant.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, heure)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (maintenant.timeInMillis >= cible.timeInMillis) {
                try {
                    val cloture = repository.executerClotureGlobale()
                    Log.i("AutoClotureWorker", "Fermeture automatique exécutée (heure configurée=${infos.heureFermetureAuto}, cloture=${cloture.idCloture})")
                } catch (e: IllegalStateException) {
                    // Clôture du jour déjà générée (fermeture manuelle plus tôt) : rien à faire.
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("AutoClotureWorker", "Échec de la vérification de fermeture automatique", e)
            Result.success() // ne pas boucler indéfiniment sur une erreur de config/stock
        }
    }
}
