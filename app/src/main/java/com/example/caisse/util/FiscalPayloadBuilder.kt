package com.example.caisse.util

import com.example.caisse.data.Ticket
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object FiscalPayloadBuilder {

    private val decimalFormat = DecimalFormat("0.00", DecimalFormatSymbols(Locale.US))

    fun buildCanonicalPayload(
        sequence: Long,
        invoiceNumber: String,
        issuedAtMillis: Long,
        items: List<Ticket>,
        total: Double,
        currency: String?,
        previousHash: String
    ): String {
        val lines = items.joinToString("|") { ticket ->
            val productName = ticket.produit.nom
                .replace("\n", " ")
                .trim()

            val unitPrice = decimalFormat.format(ticket.produit.prix)
            val lineTotal = decimalFormat.format(ticket.produit.prix * ticket.quantity)

            listOf(
                productName,
                ticket.quantity.toString(),
                unitPrice,
                lineTotal
            ).joinToString(";")
        }

        return listOf(
            "seq=$sequence",
            "invoice=$invoiceNumber",
            "issuedAt=$issuedAtMillis",
            "currency=${currency ?: ""}",
            "total=${decimalFormat.format(total)}",
            "prevHash=$previousHash",
            "items=$lines"
        ).joinToString("||")
    }
}