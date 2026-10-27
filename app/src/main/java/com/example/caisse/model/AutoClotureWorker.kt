package com.example.caisse.model

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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
 * Seule une session ouverte AVANT l'heure configurée du jour est clôturée : une caisse
 * ouverte à 10:00 avec une fermeture réglée à 05:00 reste ouverte jusqu'au lendemain 05:00
 * (sinon elle était clôturée dans les 15 min suivant son ouverture).
 *
 * La clôture couvre toutes les ventes depuis la clôture précédente et porte la date
 * d'ouverture de la session (voir `CaisseRepository.periodeClotureEnCours`) : une caisse
 * ouverte le 23 au soir et fermée seule le 24 à 05:00 donne la clôture du 23, ventes d'après
 * minuit comprises.
 *
 * Pas de déclenchement en double : la clôture ferme la session (`estCaisseOuverte` devient
 * faux). Si la date est déjà clôturée, `genererClotureJournaliere()` lève une exception,
 * avalée ici.
 */
class AutoClotureWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        val repository = CaisseRepository.create(applicationContext)

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

            // Date d'ouverture absente ou illisible : session ouverte par une ancienne version,
            // donc forcément avant l'heure de fermeture du jour -> on la traite comme telle.
            val ouvertureMillis = repository.venteDao.getEtatCaisse()?.dateOuverture
                ?.let { runCatching { java.time.LocalDateTime.parse(it) }.getOrNull() }
                ?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
            val ouverteAvantCible = ouvertureMillis == null || ouvertureMillis < cible.timeInMillis

            if (maintenant.timeInMillis >= cible.timeInMillis && ouverteAvantCible) {
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
