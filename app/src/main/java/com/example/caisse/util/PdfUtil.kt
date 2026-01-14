package com.example.piece.util


import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.piece.data.Recette
import com.example.piece.data.Voiture
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PdfUtil {

    fun generateAndShareWeeklyReport(
        context: Context,
        recettes: List<Recette>,
        voitures: List<Voiture>,
        devise: String
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 standard (points)
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        val paint = Paint()

        // --- Configuration Design ---
        val colorPrimary = Color.rgb(40, 53, 147) // Bleu foncé Pro
        val colorAccent = Color.rgb(232, 234, 246) // Gris/Bleu très clair pour fonds
        val colorText = Color.BLACK
        val colorGreen = Color.rgb(46, 125, 50)
        val colorRed = Color.rgb(198, 40, 40)

        var yPos = 40f
        val margin = 40f
        val pageWidth = 595f

        // --- 1. Filtrer les données (Semaine en cours) ---
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        val startOfWeek = cal.timeInMillis

        // On prend tout ou juste la semaine ? Ici on filtre pour la semaine comme demandé
        val weeklyOps = recettes.filter { it.date >= startOfWeek }
        val totalRecette = weeklyOps.filter { it.isRecette }.sumOf { it.amount }
        val totalDepense = weeklyOps.filter { !it.isRecette }.sumOf { it.amount }
        val solde = totalRecette - totalDepense

        // --- 2. En-tête Global ---
        // Titre
        paint.color = colorPrimary
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("RAPPORT HEBDOMADAIRE", margin, yPos, paint)

        yPos += 30f
        paint.textSize = 12f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.GRAY
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateStr = "Semaine du ${sdf.format(Date(startOfWeek))} au ${sdf.format(Date())}"
        canvas.drawText(dateStr, margin, yPos, paint)

        yPos += 30f

        // --- 3. Cadre Résumé Financier ---
        val boxHeight = 60f
        val boxWidth = pageWidth - (2 * margin)

        // Fond du cadre
        paint.color = colorAccent
        canvas.drawRect(margin, yPos, margin + boxWidth, yPos + boxHeight, paint)

        // Textes du cadre
        val colWidth = boxWidth / 3

        fun drawStat(title: String, amount: Double, x: Float, color: Int) {
            paint.color = Color.GRAY
            paint.textSize = 10f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(title, x, yPos + 20f, paint)

            paint.color = color
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(formatPrice(amount,devise), x, yPos + 45f, paint)
        }

        drawStat("TOTAL RECETTES", totalRecette, margin + (colWidth/2), colorGreen)
        drawStat("TOTAL DÉPENSES", totalDepense, margin + (colWidth * 1.5f), colorRed)
        drawStat("SOLDE GLOBAL", solde, margin + (colWidth * 2.5f), colorPrimary)

        paint.textAlign = Paint.Align.LEFT // Reset align
        yPos += boxHeight + 40f

        // --- 4. Détail par Voiture ---
        val opsByVoiture = weeklyOps.groupBy { it.voitureId }

        opsByVoiture.forEach { (voitureId, ops) ->
            val voitureName = voitures.find { it.id == voitureId }?.name ?: "Voiture Inconnue"

            // Vérifier si on doit changer de page
            if (yPos > 750) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPos = 40f
            }

            // Titre Voiture
            paint.color = colorPrimary
            paint.textSize = 16f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Vehicule : $voitureName", margin, yPos, paint)

            // Ligne séparatrice
            paint.strokeWidth = 1f
            canvas.drawLine(margin, yPos + 10f, pageWidth - margin, yPos + 10f, paint)
            yPos += 30f

            // En-têtes du tableau
            paint.color = Color.DKGRAY
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Date", margin, yPos, paint)
            canvas.drawText("Libellé", margin + 80, yPos, paint)
            canvas.drawText("Type", margin + 300, yPos, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Montant", pageWidth - margin, yPos, paint)
            paint.textAlign = Paint.Align.LEFT

            yPos += 15f

            // Lignes du tableau
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 10f
            paint.color = colorText

            var subTotal = 0.0

            ops.forEach { op ->
                if (yPos > 800) {
                    pdfDocument.finishPage(page)
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = 40f
                }

                val opDate = sdf.format(Date(op.date))
                val type = if (op.isRecette) "Recette" else "Dépense"
                val amountColor = if (op.isRecette) colorGreen else colorRed
                val sign = if (op.isRecette) "+" else "-"

                // Calcul sous-total (recette - depense) ou simple somme ?
                // Généralement dans un export comptable on veut voir l'impact sur le solde
                if(op.isRecette) subTotal += op.amount else subTotal -= op.amount

                canvas.drawText(opDate, margin, yPos, paint)

                // Tronquer le nom si trop long
                val safeName = if (op.name.length > 35) op.name.take(35) + "..." else op.name
                canvas.drawText(safeName, margin + 80, yPos, paint)

                canvas.drawText(type, margin + 300, yPos, paint)

                paint.textAlign = Paint.Align.RIGHT
                paint.color = amountColor
                canvas.drawText("$sign ${formatPrice(op.amount,devise)}", pageWidth - margin, yPos, paint)
                paint.color = colorText // Reset
                paint.textAlign = Paint.Align.LEFT

                yPos += 15f
            }

            // Sous-total voiture
            yPos += 5f
            paint.strokeWidth = 0.5f
            paint.color = Color.LTGRAY
            canvas.drawLine(margin + 200, yPos, pageWidth - margin, yPos, paint)
            yPos += 15f

            paint.textAlign = Paint.Align.RIGHT
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = colorPrimary
            canvas.drawText("Solde   ${voitureName}: ${formatPrice(subTotal,devise )}  ", pageWidth - margin, yPos, paint)
            paint.textAlign = Paint.Align.LEFT

            yPos += 40f // Espace entre les voitures
        }

        pdfDocument.finishPage(page)

        // --- 5. Sauvegarde et Partage ---
        val file = File(context.cacheDir, "Rapport_Flotte_Auto.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        pdfDocument.close()

        sharePdf(context, file)
    }

    private fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Partager le rapport PDF"))
    }
}