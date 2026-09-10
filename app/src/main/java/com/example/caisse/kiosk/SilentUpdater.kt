package com.example.caisse.kiosk

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Mise à jour silencieuse de l'application, possible car l'app est Device Owner.
 *
 * Pré-requis côté serveur (Firestore) — document `config/app_android` :
 *   - versionCode  (Number)  : le versionCode de la dernière version publiée
 *   - versionName  (String)  : ex. "1.6"
 *   - apkUrl       (String)  : URL HTTPS directe vers l'APK SIGNÉ (même keystore !)
 *
 * L'APK DOIT être signé avec le même keystore que la version installée,
 * sinon l'installation échoue (signatures différentes).
 */
object SilentUpdater {

    private const val TAG = "SilentUpdater"
    private const val CONFIG_COLLECTION = "config"
    private const val CONFIG_DOC = "app_android"
    private const val DEVICES_COLLECTION = "devices"
    private const val INSTALL_ACTION = "com.example.caisse.INSTALL_RESULT"

    /** Dernière raison d'échec détaillée (téléchargement), pour un message d'erreur précis côté UI. */
    private var lastDownloadError: String? = null

    data class UpdateInfo(
        val versionCode: Long,
        val versionName: String,
        val apkUrl: String,
        /** SHA-256 (hex) attendu de l'APK. Fortement recommandé : sans lui, seule la validation
         *  de package/signature protège contre une URL détournée. */
        val sha256: String? = null,
    )

    /**
     * Identifiant unique et stable de l'appareil (ANDROID_ID).
     * À afficher en maintenance pour pouvoir créer son document `devices/{id}` sur Firestore.
     */
    @SuppressLint("HardwareIds")
    fun deviceId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

    /** versionCode actuellement installé. */
    private fun currentVersionCode(context: Context): Long {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            pInfo.longVersionCode
        else
            @Suppress("DEPRECATION") pInfo.versionCode.toLong()
    }

    /**
     * Vérifie sur Firestore s'il existe une version cible pour CE pad.
     *
     * Priorité :
     *   1. `devices/{deviceId}` — surcharge par appareil (ciblage d'un pad précis).
     *      - peut contenir `targetVersionCode` + `apkUrl` (+ `versionName`)
     *      - si `paused == true`, aucune mise à jour pour ce pad.
     *   2. `config/app_android` — version globale (tous les pads).
     *
     * Une mise à jour n'est proposée que si la version cible > version installée.
     * (cible > installée → mise à jour ; cible < installée → ignoré, pas de downgrade auto).
     */
    suspend fun checkForUpdate(context: Context): UpdateInfo? {
        return try {
            val db = FirebaseFirestore.getInstance()
            val current = currentVersionCode(context)

            // 1. Surcharge par appareil
            val deviceSnap = db.collection(DEVICES_COLLECTION)
                .document(deviceId(context))
                .get()
                .await()

            if (deviceSnap.exists()) {
                if (deviceSnap.getBoolean("paused") == true) {
                    Log.i(TAG, "Mises à jour en pause pour ce pad.")
                    return null
                }
                val devCode = deviceSnap.getLong("targetVersionCode")
                val devUrl = deviceSnap.getString("apkUrl")
                if (devCode != null && devUrl != null) {
                    val devName = deviceSnap.getString("versionName") ?: ""
                    val devSha = deviceSnap.getString("sha256")
                    return if (devCode > current) UpdateInfo(devCode, devName, devUrl, devSha) else null
                }
                // Document présent mais sans cible explicite → on retombe sur la config globale.
            }

            // 2. Config globale
            val snap = db.collection(CONFIG_COLLECTION).document(CONFIG_DOC).get().await()
            val remoteCode = snap.getLong("versionCode") ?: return null
            val apkUrl = snap.getString("apkUrl") ?: return null
            val versionName = snap.getString("versionName") ?: ""
            val sha256 = snap.getString("sha256")

            if (remoteCode > current) UpdateInfo(remoteCode, versionName, apkUrl, sha256) else null
        } catch (e: Exception) {
            Log.e(TAG, "checkForUpdate a échoué", e)
            null
        }
    }

    /**
     * Télécharge l'APK dans le cache interne et retourne le fichier, ou null en cas d'échec.
     * Valide que le contenu est bien un APK (signature ZIP "PK") et non une page HTML
     * (cas typique d'un lien Google Drive "/view" au lieu d'une URL directe).
     */
    suspend fun downloadApk(context: Context, url: String): File? = withContext(Dispatchers.IO) {
        lastDownloadError = null
        try {
            val dest = File(context.cacheDir, "update.apk")
            if (dest.exists()) dest.delete()

            // Cas Firebase Storage : on résout l'URI gs:// (ou un lien firebasestorage) via le SDK.
            if (url.startsWith("gs://") || url.contains("firebasestorage")) {
                try {
                    FirebaseStorage.getInstance()
                        .getReferenceFromUrl(url)
                        .getFile(dest)
                        .await()
                } catch (e: StorageException) {
                    val signedIn = FirebaseAuth.getInstance().currentUser != null
                    lastDownloadError = if (e.errorCode == StorageException.ERROR_NOT_AUTHORIZED) {
                        "Permission refusée par Firebase Storage (règles de sécurité) sur : $url" +
                            if (!signedIn) " — aucun utilisateur connecté." else " — utilisateur connecté mais non autorisé par les règles."
                    } else {
                        "Erreur Firebase Storage (${e.errorCode}) : ${e.message}"
                    }
                    Log.e(TAG, lastDownloadError!!, e)
                    return@withContext null
                }
                return@withContext validateApk(dest)
            }

            // HTTPS obligatoire : en HTTP clair, un attaquant sur le réseau pourrait substituer
            // l'APK pendant le téléchargement (l'installation est silencieuse, Device Owner).
            if (!url.startsWith("https://", ignoreCase = true)) {
                Log.e(TAG, "URL de mise à jour refusée (HTTPS requis) : $url")
                return@withContext null
            }

            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 60_000
                instanceFollowRedirects = true
            }
            conn.connect()
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Téléchargement HTTP ${conn.responseCode}")
                return@withContext null
            }
            val contentType = conn.contentType ?: ""
            if (contentType.contains("text/html", ignoreCase = true)) {
                Log.e(TAG, "L'URL renvoie du HTML, pas un APK : $url")
                return@withContext null
            }
            conn.inputStream.use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
            conn.disconnect()
            validateApk(dest)
        } catch (e: Exception) {
            Log.e(TAG, "downloadApk a échoué", e)
            null
        }
    }

    /** Vérifie que le fichier est bien un APK (un ZIP commence par les octets "PK"). */
    private fun validateApk(file: File): File? {
        if (!file.exists() || file.length() < 2) {
            Log.e(TAG, "Fichier téléchargé vide ou inexistant.")
            return null
        }
        val header = ByteArray(2)
        file.inputStream().use { it.read(header) }
        return if (header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()) {
            file
        } else {
            Log.e(TAG, "Fichier invalide (pas un APK). Vérifie l'apkUrl.")
            file.delete()
            null
        }
    }

    /** SHA-256 (hex minuscule) du fichier, lu en streaming. */
    private fun fileSha256(file: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n <= 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Contrôles d'intégrité avant installation silencieuse :
     *  1. checksum SHA-256 si fourni dans la config (protège d'une URL/fichier substitué) ;
     *  2. le package de l'APK doit être EXACTEMENT le nôtre — sans ce contrôle, une apkUrl
     *     détournée ferait installer une application arbitraire en silence (Device Owner).
     * La signature est ensuite vérifiée par Android lui-même : la mise à jour d'un package
     * installé est refusée par le système si le certificat de signature diffère.
     */
    private fun verifyBeforeInstall(context: Context, apk: File, expectedSha256: String?): String? {
        if (expectedSha256 != null) {
            val actual = fileSha256(apk)
            if (!actual.equals(expectedSha256.trim(), ignoreCase = true)) {
                return "Checksum SHA-256 invalide (attendu=$expectedSha256, obtenu=$actual)."
            }
        } else {
            Log.w(TAG, "Aucun champ sha256 dans la config : checksum non vérifié (à ajouter).")
        }

        val info = context.packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
            ?: return "APK illisible par le PackageManager."
        if (info.packageName != context.packageName) {
            return "Package inattendu : ${info.packageName} (attendu ${context.packageName})."
        }
        return null // OK
    }

    /**
     * Installe l'APK en silence via PackageInstaller (aucun prompt car Device Owner).
     * L'app se relance toute seule après mise à jour.
     */
    fun installApk(context: Context, apk: File): Boolean {
        return try {
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            )
            params.setAppPackageName(context.packageName)

            val sessionId = installer.createSession(params)
            installer.openSession(sessionId).use { session ->
                apk.inputStream().use { input ->
                    session.openWrite("caisse_update", 0, apk.length()).use { out ->
                        input.copyTo(out)
                        session.fsync(out)
                    }
                }

                val intent = Intent(INSTALL_ACTION).setPackage(context.packageName)
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
                else
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT
                val pending = android.app.PendingIntent.getBroadcast(
                    context, sessionId, intent, flags
                )
                session.commit(pending.intentSender)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "installApk a échoué", e)
            false
        }
    }

    /**
     * Orchestration complète : vérifie, télécharge, installe.
     * Retourne un message d'état lisible pour l'UI de maintenance.
     */
    suspend fun runUpdate(context: Context): String {
        val info = checkForUpdate(context)
            ?: return "Aucune mise à jour disponible."

        val apk = downloadApk(context, info.apkUrl)
            ?: return lastDownloadError
                ?: "Échec du téléchargement : l'URL n'est pas un APK direct en HTTPS " +
                    "(évite les liens Google Drive /view, utilise Firebase Storage)."

        val integrityError = verifyBeforeInstall(context, apk, info.sha256)
        if (integrityError != null) {
            apk.delete()
            Log.e(TAG, "Installation refusée : $integrityError")
            return "Installation refusée : $integrityError"
        }

        val ok = installApk(context, apk)
        return if (ok)
            "Installation de la version ${info.versionName} en cours…"
        else
            "Échec de l'installation."
    }
}
