package com.example.piece.util


import java.text.NumberFormat
import java.util.Locale

fun formatPrice(
    amount: Double,
    devise: String?
): String {
    return when (devise) {

        // FCFA : entier, séparateur espace
        "FCFA", "XOF" -> {
            val formatted = NumberFormat
                .getInstance(Locale.US)
                .format(amount.toInt())
                .replace(",", " ")

            "$formatted $devise"
        }

        // Euro : 2 décimales
        "€", "EUR" -> {
            val formatted = String.format(Locale.FRANCE, "%.2f", amount)
            "$formatted $devise"
        }

        // Fallback générique
        else -> {
            val formatted = String.format(Locale.US, "%.2f", amount)
            "$formatted ${devise ?: ""}"
        }
    }
}
