package com.example.caisse.kiosk

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Active / désactive à distance le blocage réseau ([BlockingVpnService]) depuis Firestore,
 * sur le même modèle que le champ `paused` des mises à jour :
 *
 *   - `devices/{ANDROID_ID}` (prioritaire, ce pad uniquement)
 *   - `config/app_android`   (tous les pads)
 *
 * Champs (booléens, facultatifs) :
 *   - `vpnBlocking` : true = seule la caisse a le réseau. Absent partout = blocage désactivé.
 *   - `vpnLockdown` : mode lockdown du VPN. Absent partout = [KioskManager.VPN_LOCKDOWN].
 *
 * Écoute en temps réel : un changement dans la console Firebase est appliqué en quelques
 * secondes. La caisse étant exclue du VPN, elle peut toujours relire Firestore pour lever
 * le blocage. Sans réseau ni cache, rien n'est modifié : le système conserve le dernier état.
 */
object NetworkBlockController {

    private const val TAG = "NetworkBlockController"

    /** État appliqué, affiché dans l'écran de maintenance (null = pas encore lu). */
    private val _blockingEnabled = MutableStateFlow<Boolean?>(null)
    val blockingEnabled: StateFlow<Boolean?> = _blockingEnabled.asStateFlow()

    private var started = false
    private var deviceSnap: DocumentSnapshot? = null
    private var globalSnap: DocumentSnapshot? = null
    private var applied: Pair<Boolean, Boolean>? = null

    /** Démarre l'écoute Firestore (une seule fois, sans effet si pas Device Owner). */
    @Synchronized
    fun start(context: Context) {
        if (started || !KioskManager.isDeviceOwner(context)) return
        started = true
        val appContext = context.applicationContext
        val db = FirebaseFirestore.getInstance()

        db.collection(SilentUpdater.DEVICES_COLLECTION)
            .document(SilentUpdater.deviceId(appContext))
            .addSnapshotListener { snap, e ->
                if (e != null) { Log.e(TAG, "Lecture devices/ échouée", e); return@addSnapshotListener }
                deviceSnap = snap
                evaluate(appContext)
            }

        db.collection(SilentUpdater.CONFIG_COLLECTION)
            .document(SilentUpdater.CONFIG_DOC)
            .addSnapshotListener { snap, e ->
                if (e != null) { Log.e(TAG, "Lecture config/ échouée", e); return@addSnapshotListener }
                globalSnap = snap
                evaluate(appContext)
            }
    }

    /** Résout la valeur effective (pad > global > défaut) et l'applique si elle a changé. */
    @Synchronized
    private fun evaluate(context: Context) {
        // Attendre les deux documents pour ne pas appliquer un état partiel au démarrage.
        val device = deviceSnap ?: return
        val global = globalSnap ?: return

        fun flag(field: String): Boolean? =
            device.takeIf { it.exists() }?.getBoolean(field)
                ?: global.takeIf { it.exists() }?.getBoolean(field)

        val enabled = flag("vpnBlocking") ?: false
        val lockdown = flag("vpnLockdown") ?: KioskManager.VPN_LOCKDOWN
        val target = enabled to lockdown
        if (target == applied) return

        if (KioskManager.setNetworkBlocking(context, enabled, lockdown)) {
            applied = target
            _blockingEnabled.value = enabled
            Log.i(TAG, "Blocage réseau ${if (enabled) "activé" else "désactivé"} (lockdown=$lockdown)")
        }
    }
}
