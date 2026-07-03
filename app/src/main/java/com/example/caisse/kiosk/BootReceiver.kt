package com.example.caisse.kiosk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Lance l'application automatiquement :
 *  - au démarrage de l'appareil (BOOT_COMPLETED) ;
 *  - après une mise à jour silencieuse de l'app (MY_PACKAGE_REPLACED), pour que le pad
 *    revienne sur la caisse sans intervention.
 *
 * Complète le launcher HOME persistant posé par [KioskManager.applyKioskPolicies] : même si
 * la politique HOME n'a pas encore été appliquée (premier boot après provisionnement), le
 * pad démarre quand même sur la caisse. Le verrouillage LockTask est ensuite fait par
 * MainActivity.onResume().
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        Log.i("BootReceiver", "Lancement de la caisse suite à : $action")
        val launch = Intent(context, com.example.caisse.MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        try {
            context.startActivity(launch)
        } catch (e: Exception) {
            Log.e("BootReceiver", "Impossible de lancer MainActivity", e)
        }
    }
}
