package com.example.caisse.util


import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64
import com.example.caisse.data.Vente
import com.example.caisse.data.VenteLigne

object PasswordHasher {

    // PBKDF2 : contrairement à un simple SHA-256 (calculable des milliards de fois/seconde sur
    // GPU), le coût des itérations rend le brute-force d'un hash exfiltré impraticable — d'autant
    // que ce hash est synchronisé dans Firestore. SHA1 (et pas SHA256) car dispo dès minSdk 23 ;
    // avec ce nombre d'itérations et une sortie de 256 bits, c'est l'état de l'art mobile.
    private const val PBKDF2_ITERATIONS = 120_000
    // Un PIN à 4 chiffres n'a que 10 000 combinaisons possibles : un coût PBKDF2 élevé ne change
    // presque rien à la résistance au brute-force (l'espace de clés est déjà minuscule) mais rend
    // chaque connexion vendeur perceptiblement lente ("ça mouline"). Coût réduit en conséquence.
    private const val PIN_PBKDF2_ITERATIONS = 10_000
    private const val PBKDF2_PREFIX = "pbkdf2"

    fun generateSalt(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    /** Format produit : "pbkdf2:<itérations>:<base64>". */
    fun hash(password: String, salt: String): String =
        pbkdf2(password, salt, PBKDF2_ITERATIONS)

    /** Comme [hash], mais avec un coût réduit adapté à un PIN à 4 chiffres. */
    fun hashPin(pin: String, salt: String): String =
        pbkdf2(pin, salt, PIN_PBKDF2_ITERATIONS)

    private fun pbkdf2(password: String, salt: String, iterations: Int): String {
        val spec = javax.crypto.spec.PBEKeySpec(
            password.toCharArray(),
            Base64.decode(salt, Base64.NO_WRAP),
            iterations,
            256
        )
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val bytes = factory.generateSecret(spec).encoded
        return "$PBKDF2_PREFIX:$iterations:" + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /** Ancien format (un tour de SHA-256) : conservé pour vérifier les hash existants. */
    private fun legacySha256(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((password + salt).toByteArray())
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun verify(
        inputPassword: String,
        storedHash: String,
        storedSalt: String
    ): Boolean {
        val computed = if (storedHash.startsWith("$PBKDF2_PREFIX:")) {
            val iterations = storedHash.split(":").getOrNull(1)?.toIntOrNull() ?: return false
            pbkdf2(inputPassword, storedSalt, iterations)
        } else {
            legacySha256(inputPassword, storedSalt)
        }
        // Comparaison en temps constant : un == ordinaire laisse fuiter la position du premier
        // caractère différent (attaque par mesure de temps).
        return MessageDigest.isEqual(computed.toByteArray(), storedHash.toByteArray())
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
