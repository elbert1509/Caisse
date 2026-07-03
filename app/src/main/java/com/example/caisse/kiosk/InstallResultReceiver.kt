package com.example.caisse.kiosk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log

/**
 * Reçoit le résultat de l'installation silencieuse lancée par [SilentUpdater].
 * En Device Owner, le statut est normalement STATUS_SUCCESS sans interaction.
 */
class InstallResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE
        )
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

        when (status) {
            PackageInstaller.STATUS_SUCCESS ->
                Log.i("InstallResultReceiver", "Mise à jour installée avec succès.")

            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // Ne devrait pas arriver en Device Owner. Au cas où, on relance l'intent de confirmation.
                Log.w("InstallResultReceiver", "Action utilisateur requise (pas en mode silencieux ?).")
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                confirm?.let { runCatching { context.startActivity(it) } }
            }

            else ->
                Log.e("InstallResultReceiver", "Échec de l'installation ($status) : $message")
        }
    }
}
