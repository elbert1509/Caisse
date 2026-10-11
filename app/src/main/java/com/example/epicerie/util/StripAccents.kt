package com.example.epicerie.util

import java.text.Normalizer
import java.util.UUID

fun StripAccents(input: String): String {
    val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
    return normalized.replace("\\p{Mn}+".toRegex(), "")
}
fun invoiceNoFromId(id: UUID): String {
    // Exemple: INV-9F3A1C2B (8 chars, lisible)
    val short = id.toString().replace("-", "").takeLast(8).uppercase()
    return "INV-$short"
}

/** NF525 Axe B — Numéro de ticket séquentiel et ininterrompu. */
fun formatTicketNumber(sequenceNumber: Long): String = "TKT-%06d".format(sequenceNumber)

const val PKG = "com.example.epicerie"

/** URI pointant vers un drawable embarqué dans l'app (image produit locale). */
fun drawableUri(name: String) =
    "android.resource://$PKG/drawable/$name"