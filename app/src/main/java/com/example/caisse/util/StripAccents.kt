package com.example.caisse.util

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