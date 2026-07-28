package com.example.caisse.util

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.caisse.data.ProductReport
import com.example.caisse.data.Produit
import com.example.caisse.data.ShopInfos
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Rapports de ventes en PDF (journalier / hebdomadaire / mensuel), générés nativement
 * (android.graphics.pdf, aucune dépendance).
 *
 * Contenu : en-tête boutique (fiche magasin synchronisée), CA total dans la devise de la
 * boutique, tableau des produits vendus (quantité, montant, stock), pagination automatique.
 */
object PdfReportGenerator {

    // Format A4 en points (72 dpi)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val BOTTOM_LIMIT = PAGE_HEIGHT - 50f

    private val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)

    fun generateRapportProduits(
        destDir: File,
        infos: ShopInfos?,
        title: String,
        items: List<ProductReport>,
        total: Double,
    ): File {
        val devise = infos?.devise?.ifBlank { null } ?: "FCFA"

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 18f
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 12f
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f }
        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f; color = Color.DKGRAY }
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

        // Colonnes du tableau
        val colProduit = MARGIN
        val colQte = MARGIN + 240f
        val colMontant = MARGIN + 320f
        val colStock = PAGE_WIDTH - MARGIN - 50f

        val doc = PdfDocument()
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        fun drawTableHeader() {
            page!!.canvas.apply {
                drawText("Produit", colProduit, y, sectionPaint)
                drawText("Quantité", colQte, y, sectionPaint)
                drawText("Montant", colMontant, y, sectionPaint)
                drawText("Stock", colStock, y, sectionPaint)
            }
            y += 12f
            page!!.canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 14f
        }

        fun newPage(withTableHeader: Boolean) {
            page?.let { doc.finishPage(it) }
            pageNumber++
            page = doc.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            )
            y = MARGIN
            page!!.canvas.drawText("Page $pageNumber", PAGE_WIDTH - 75f, PAGE_HEIGHT - 20f, smallPaint)
            // Sur les pages suivantes, on répète l'en-tête du tableau pour rester lisible.
            if (withTableHeader) drawTableHeader()
        }

        fun montant(v: Double): String =
            if (v % 1.0 == 0.0) "%,.0f %s".format(Locale.FRANCE, v, devise)
            else "%,.2f %s".format(Locale.FRANCE, v, devise)

        newPage(withTableHeader = false)
        val canvas = { page!!.canvas }

        // ---------- En-tête boutique ----------
        canvas().drawText(infos?.name?.ifBlank { null } ?: "Ma Caisse", MARGIN, y + 8f, titlePaint)
        y += 26f
        infos?.let {
            if (it.address.isNotBlank()) { canvas().drawText(it.address, MARGIN, y, smallPaint); y += 12f }
            val contact = listOf(it.phone, it.email).filter { c -> c.isNotBlank() }.joinToString(" — ")
            if (contact.isNotBlank()) { canvas().drawText(contact, MARGIN, y, smallPaint); y += 12f }
            if (it.siret.isNotBlank()) { canvas().drawText("SIRET : ${it.siret}", MARGIN, y, smallPaint); y += 12f }
        }
        y += 4f
        canvas().drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 20f

        // ---------- Titre du rapport ----------
        canvas().drawText(title, MARGIN, y, Paint(titlePaint).apply { textSize = 15f })
        y += 16f
        canvas().drawText("Généré le ${dateFmt.format(Date())}", MARGIN, y, smallPaint)
        y += 18f
        canvas().drawText("Chiffre d'affaires : ${montant(total)}", MARGIN, y, sectionPaint)
        y += 10f
        canvas().drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 16f

        // ---------- Tableau des produits ----------
        drawTableHeader()
        if (items.isEmpty()) {
            canvas().drawText("Aucune vente sur la période.", MARGIN, y, textPaint)
            y += 14f
        }
        for (r in items) {
            if (y > BOTTOM_LIMIT) newPage(withTableHeader = true)
            canvas().apply {
                drawText(r.productName.take(38), colProduit, y, textPaint)
                drawText(r.totalQuantity.toString(), colQte, y, textPaint)
                drawText(montant(r.revenue), colMontant, y, textPaint)
                drawText(r.productStock.toString(), colStock, y, textPaint)
            }
            y += 15f
        }

        // ---------- Total en pied de tableau ----------
        if (y > BOTTOM_LIMIT - 20f) newPage(withTableHeader = false)
        y += 4f
        canvas().drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 16f
        canvas().drawText("TOTAL : ${montant(total)}", colMontant, y, sectionPaint)

        page?.let { doc.finishPage(it) }

        val safeTitle = title.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "_").trim('_')
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.FRANCE).format(Date())
        val file = File(destDir, "${safeTitle}_$stamp.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    /**
     * État du stock des produits actifs : en-tête boutique, tableau Produit / Stock,
     * total des unités en pied, pagination automatique.
     */
    fun generateEtatStock(
        destDir: File,
        infos: ShopInfos?,
        produits: List<Produit>,
    ): File {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 18f
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 12f
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f }
        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f; color = Color.DKGRAY }
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

        // Colonnes du tableau
        val colProduit = MARGIN
        val colStock = PAGE_WIDTH - MARGIN - 60f

        val doc = PdfDocument()
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        fun drawTableHeader() {
            page!!.canvas.apply {
                drawText("Produit", colProduit, y, sectionPaint)
                drawText("Stock", colStock, y, sectionPaint)
            }
            y += 12f
            page!!.canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 14f
        }

        fun newPage(withTableHeader: Boolean) {
            page?.let { doc.finishPage(it) }
            pageNumber++
            page = doc.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            )
            y = MARGIN
            page!!.canvas.drawText("Page $pageNumber", PAGE_WIDTH - 75f, PAGE_HEIGHT - 20f, smallPaint)
            if (withTableHeader) drawTableHeader()
        }

        val totalUnits = produits.sumOf { maxOf(0, it.stock) }

        newPage(withTableHeader = false)
        val canvas = { page!!.canvas }

        // ---------- En-tête boutique ----------
        canvas().drawText(infos?.name?.ifBlank { null } ?: "Ma Caisse", MARGIN, y + 8f, titlePaint)
        y += 26f
        infos?.let {
            if (it.address.isNotBlank()) { canvas().drawText(it.address, MARGIN, y, smallPaint); y += 12f }
            val contact = listOf(it.phone, it.email).filter { c -> c.isNotBlank() }.joinToString(" — ")
            if (contact.isNotBlank()) { canvas().drawText(contact, MARGIN, y, smallPaint); y += 12f }
            if (it.siret.isNotBlank()) { canvas().drawText("SIRET : ${it.siret}", MARGIN, y, smallPaint); y += 12f }
        }
        y += 4f
        canvas().drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 20f

        // ---------- Titre ----------
        canvas().drawText("État du stock", MARGIN, y, Paint(titlePaint).apply { textSize = 15f })
        y += 16f
        canvas().drawText("Généré le ${dateFmt.format(Date())}", MARGIN, y, smallPaint)
        y += 18f
        canvas().drawText(
            "Produits actifs : ${produits.size} — Unités en stock : $totalUnits",
            MARGIN, y, sectionPaint
        )
        y += 10f
        canvas().drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 16f

        // ---------- Tableau du stock ----------
        drawTableHeader()
        if (produits.isEmpty()) {
            canvas().drawText("Aucun produit actif.", MARGIN, y, textPaint)
            y += 14f
        }
        for (p in produits) {
            if (y > BOTTOM_LIMIT) newPage(withTableHeader = true)
            canvas().apply {
                drawText(p.nom.take(50), colProduit, y, textPaint)
                drawText(maxOf(0, p.stock).toString(), colStock, y, textPaint)
            }
            y += 15f
        }

        // ---------- Total en pied de tableau ----------
        if (y > BOTTOM_LIMIT - 20f) newPage(withTableHeader = false)
        y += 4f
        canvas().drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 16f
        canvas().drawText("TOTAL", colProduit, y, sectionPaint)
        canvas().drawText("$totalUnits", colStock, y, sectionPaint)

        page?.let { doc.finishPage(it) }

        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.FRANCE).format(Date())
        val file = File(destDir, "etat_stock_$stamp.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
