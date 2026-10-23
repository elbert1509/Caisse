package com.example.caisse.util

import android.app.Application
import android.content.Context
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.example.caisse.data.CaisseRepository
import com.example.caisse.data.TypeEvenement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Inscrit dans le journal des événements (JET) :
 *  - les crashs (exception non rattrapée, quel que soit le thread) → `CRASH_APPLICATION` ;
 *  - les gels de l'interface (thread principal bloqué plus de [FREEZE_THRESHOLD_MS]) →
 *    `GEL_APPLICATION`, avec la durée et la pile du thread principal.
 *
 * Un processus qui crashe ne peut pas écrire en base de façon fiable : l'événement est d'abord
 * enregistré de façon synchrone dans des SharedPreferences, puis versé au JET au démarrage
 * suivant. Idem pour un gel en cours quand l'appli est tuée ou le terminal redémarré.
 */
object CrashJournal {

    private const val TAG = "CrashJournal"
    private const val PREFS = "crash_journal"
    private const val PENDING_CRASH = "pendingCrash"
    private const val PENDING_FREEZE = "pendingFreeze"
    private const val FREEZE_THRESHOLD_MS = 5_000L
    private const val MAX_DESCRIPTION = 1_500

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun install(app: Application) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                prefs(app).edit()
                    .putString(PENDING_CRASH, "Crash le ${stamp()} (thread ${thread.name}) : ${describe(error)}")
                    .commit()
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, error)
        }
        flushPending(app)
        FreezeWatchdog(app).start()
    }

    /** Verse au JET les événements laissés par le processus précédent. */
    private fun flushPending(context: Context) {
        val prefs = prefs(context)
        val crash = prefs.getString(PENDING_CRASH, null)
        val freeze = prefs.getString(PENDING_FREEZE, null)
        if (crash == null && freeze == null) return
        scope.launch {
            try {
                val repository = CaisseRepository.create(context)
                crash?.let { repository.loggerEvenement(TypeEvenement.CRASH_APPLICATION.name, it.take(MAX_DESCRIPTION)) }
                freeze?.let {
                    repository.loggerEvenement(
                        TypeEvenement.GEL_APPLICATION.name,
                        "Gel non résolu (appli arrêtée ou terminal redémarré pendant le blocage). $it".take(MAX_DESCRIPTION)
                    )
                }
                prefs.edit().remove(PENDING_CRASH).remove(PENDING_FREEZE).apply()
            } catch (e: Exception) {
                Log.e(TAG, "Impossible de journaliser l'incident précédent", e)
            }
        }
    }

    /**
     * Poste un « battement » sur le thread principal toutes les [FREEZE_THRESHOLD_MS] : s'il
     * n'a pas été traité entre-temps, l'interface est gelée.
     */
    private class FreezeWatchdog(private val context: Context) : Thread("freeze-watchdog") {
        private val main = Handler(Looper.getMainLooper())
        @Volatile private var tick = 0L

        init { isDaemon = true }

        override fun run() {
            while (true) {
                val before = tick
                main.post { tick++ }
                try { sleep(FREEZE_THRESHOLD_MS) } catch (_: InterruptedException) { return }
                if (tick != before || Debug.isDebuggerConnected()) continue

                val start = SystemClock.uptimeMillis() - FREEZE_THRESHOLD_MS
                val details = "Début le ${stamp(System.currentTimeMillis() - FREEZE_THRESHOLD_MS)}. " +
                    "Pile du thread principal : ${frames(Looper.getMainLooper().thread.stackTrace)}"
                prefs(context).edit().putString(PENDING_FREEZE, details).commit()

                while (tick == before) {
                    try { sleep(500) } catch (_: InterruptedException) { return }
                }
                val seconds = (SystemClock.uptimeMillis() - start) / 1000
                scope.launch {
                    try {
                        CaisseRepository.create(context).loggerEvenement(
                            TypeEvenement.GEL_APPLICATION.name,
                            "Interface bloquée environ $seconds s. $details".take(MAX_DESCRIPTION)
                        )
                        prefs(context).edit().remove(PENDING_FREEZE).apply()
                    } catch (e: Exception) {
                        Log.e(TAG, "Impossible de journaliser le gel", e)
                    }
                }
            }
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun stamp(millis: Long = System.currentTimeMillis()) =
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRANCE).format(Date(millis))

    /** Exception + causes, chacune avec ses premières lignes de pile. */
    private fun describe(error: Throwable): String {
        val parts = mutableListOf<String>()
        var current: Throwable? = error
        while (current != null && parts.size < 4) {
            parts += "${current.javaClass.name}: ${current.message} @ ${frames(current.stackTrace, 6)}"
            current = current.cause.takeIf { it !== current }
        }
        return parts.joinToString(" | Cause : ")
    }

    private fun frames(trace: Array<StackTraceElement>, max: Int = 12) =
        trace.take(max).joinToString(" < ") { "${it.className.substringAfterLast('.')}.${it.methodName}:${it.lineNumber}" }
}
