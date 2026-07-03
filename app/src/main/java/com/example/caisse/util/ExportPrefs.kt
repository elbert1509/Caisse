package com.example.caisse.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stockage local (SharedPreferences) des paramètres d'export :
 *  - destinataires supplémentaires (emails / numéros WhatsApp)
 *  - identifiants SMTP de l'expéditeur (Gmail + mot de passe d'application)
 *
 * Rien n'est commité dans le code source : les identifiants restent sur le pad.
 * Le mot de passe SMTP est chiffré (AES-GCM, clé dans l'Android Keystore) : il n'apparaît
 * jamais en clair dans le fichier de prefs ni dans une sauvegarde/extraction de l'appareil.
 */
object ExportPrefs {

    private const val PREFS = "export_prefs"
    private const val KEY_EXTRA_EMAILS = "extra_emails"
    private const val KEY_EXTRA_PHONES = "extra_phones"
    private const val KEY_SMTP_EMAIL = "smtp_email"
    private const val KEY_SMTP_PASSWORD = "smtp_password"          // ancien stockage en clair
    private const val KEY_SMTP_PASSWORD_ENC = "smtp_password_enc"  // stockage chiffré "iv:ct"
    private const val KEYSTORE_ALIAS = "export_prefs_smtp"

    private fun keystoreKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KEYSTORE_ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return gen.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
                Base64.encodeToString(ct, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String? = try {
        val sep = stored.indexOf(':')
        val iv = Base64.decode(stored.substring(0, sep), Base64.NO_WRAP)
        val ct = Base64.decode(stored.substring(sep + 1), Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(ct), Charsets.UTF_8)
    } catch (_: Exception) {
        null // clé Keystore perdue (reset appareil...) : l'utilisateur re-saisira le mot de passe
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // --- Destinataires supplémentaires (séparés par des ;) ---

    fun getExtraEmails(context: Context): List<String> =
        prefs(context).getString(KEY_EXTRA_EMAILS, "")!!
            .split(";").map { it.trim() }.filter { it.isNotEmpty() }

    fun setExtraEmails(context: Context, list: List<String>) {
        prefs(context).edit()
            .putString(KEY_EXTRA_EMAILS, list.joinToString(";"))
            .apply()
    }

    fun getExtraPhones(context: Context): List<String> =
        prefs(context).getString(KEY_EXTRA_PHONES, "")!!
            .split(";").map { it.trim() }.filter { it.isNotEmpty() }

    fun setExtraPhones(context: Context, list: List<String>) {
        prefs(context).edit()
            .putString(KEY_EXTRA_PHONES, list.joinToString(";"))
            .apply()
    }

    // --- Expéditeur SMTP ---

    fun getSmtpEmail(context: Context): String =
        prefs(context).getString(KEY_SMTP_EMAIL, "") ?: ""

    fun getSmtpPassword(context: Context): String {
        val p = prefs(context)
        p.getString(KEY_SMTP_PASSWORD_ENC, null)?.let { return decrypt(it) ?: "" }
        // Migration : un ancien mot de passe stocké en clair est re-chiffré puis effacé.
        val legacy = p.getString(KEY_SMTP_PASSWORD, "") ?: ""
        if (legacy.isNotEmpty()) {
            p.edit()
                .putString(KEY_SMTP_PASSWORD_ENC, encrypt(legacy))
                .remove(KEY_SMTP_PASSWORD)
                .apply()
        }
        return legacy
    }

    fun setSmtp(context: Context, email: String, password: String) {
        prefs(context).edit()
            .putString(KEY_SMTP_EMAIL, email.trim())
            .putString(KEY_SMTP_PASSWORD_ENC, encrypt(password.trim()))
            .remove(KEY_SMTP_PASSWORD)
            .apply()
    }

    fun isSmtpConfigured(context: Context): Boolean =
        getSmtpEmail(context).isNotEmpty() && getSmtpPassword(context).isNotEmpty()
}
