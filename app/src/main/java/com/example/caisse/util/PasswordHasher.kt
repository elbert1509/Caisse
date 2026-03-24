package com.example.caisse.util


import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64
import com.example.caisse.data.Vente
import com.example.caisse.data.VenteLigne

object PasswordHasher {

    fun generateSalt(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun hash(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((password + salt).toByteArray())
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun verify(
        inputPassword: String,
        storedHash: String,
        storedSalt: String
    ): Boolean {
        val inputHash = hash(inputPassword, storedSalt)
        return inputHash == storedHash
    }
}
// Créez un fichier SecurityUtils.kt ou ajoutez dans util/
object SecurityUtils {
    fun calculateHash(vente: Vente, lignes: List<VenteLigne>): String {
        val dataToHash = "${vente.id}${vente.date}${vente.total}${vente.previousHash}" +
                lignes.joinToString("") { "${it.id}${it.quantity}${it.prixUnitaire}" }

        return java.security.MessageDigest.getInstance("SHA-256")
            .digest(dataToHash.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }
}
