package com.example.caisse.util

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

/**
 * Partage un PDF vers WhatsApp (gratuit). Ouvre WhatsApp directement sur la conversation
 * du numéro fourni, avec le PDF déjà joint : l'utilisateur n'a plus qu'à appuyer sur « Envoyer ».
 *
 * L'envoi 100 % automatique sans interaction nécessiterait la WhatsApp Cloud API (payante).
 */
object WhatsAppSender {

    private const val TAG = "WhatsAppSender"
    private const val PKG = "com.whatsapp"
    private const val PKG_BUSINESS = "com.whatsapp.w4b"

    /** Vrai si WhatsApp (ou WhatsApp Business) est installé. */
    fun isInstalled(context: Context): Boolean {
        val pm = context.packageManager
        return runCatching { pm.getPackageInfo(PKG, 0) }.isSuccess ||
                runCatching { pm.getPackageInfo(PKG_BUSINESS, 0) }.isSuccess
    }

    /**
     * Ouvre WhatsApp avec le PDF joint, ciblé sur [phone] (numéro au format international,
     * ex. "24106671234" ou "+24106671234" — les espaces et le + sont ignorés).
     * Retourne false si WhatsApp n'est pas installé.
     */
    fun shareToNumber(context: Context, file: File, phone: String, text: String): Boolean {
        val pkg = when {
            runCatching { context.packageManager.getPackageInfo(PKG, 0) }.isSuccess -> PKG
            runCatching { context.packageManager.getPackageInfo(PKG_BUSINESS, 0) }.isSuccess -> PKG_BUSINESS
            else -> return false
        }

        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val digits = phone.filter { it.isDigit() } // jid sans + ni espaces

        val intent = Intent(Intent.ACTION_SEND).apply {
            setPackage(pkg)
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Cible la conversation du numéro (ouvre directement le bon contact)
            if (digits.isNotEmpty()) putExtra("jid", "$digits@s.whatsapp.net")
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Impossible d'ouvrir WhatsApp", e)
            false
        }
    }
}
