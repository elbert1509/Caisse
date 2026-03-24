package com.example.caisse.util

import com.example.caisse.data.Cloture
import java.security.MessageDigest

object FiscalHashUtils {

    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun shortHash(hash: String, size: Int = 12): String {
        return if (hash.length <= size) hash else hash.take(size)
    }

    fun calculateClotureHash(cloture: Cloture, lastClotureHash: String): String {
        // On concatène les données critiques + le hash de la clôture précédente
        val dataToHash = "${cloture.dateCloture}" +
                "${cloture.type}" +
                "${cloture.chiffreAffaireBrut}" +
                "${cloture.grandTotalCumule}" +
                "${cloture.compteurVentes}" +
                lastClotureHash

        return java.security.MessageDigest.getInstance("SHA-256")
            .digest(dataToHash.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }
}