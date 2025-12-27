package com.example.oudeika.data

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.oudeika.model.VenteDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class DashboardViewModel(venteDao: VenteDao) : ViewModel() {

    // Get the start of the current week (Monday)
    private val startOfWeek: Long = run {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.timeInMillis
    }

                    // --- Rapports par jour (Jour / Semaine / Mois) ---
                            /*Rapports par jour*/
    private fun startOfDay(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    private fun endOfDay(): Long = startOfDay() + 24L*60*60*1000

    // --- Rapports par produit (Jour / Semaine / Mois) ---
    val productReportToday: StateFlow<List<ProductReport>> =
        venteDao.getProductReportBetween(startOfDay(), endOfDay())
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val revenueTodayBounded: StateFlow<Double> =
        venteDao.getTotalRevenueBetween(startOfDay(), endOfDay())
            .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)


                /* Rapports par hebdo */

    private fun startOfWeek(): Long = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    private fun endOfWeek(): Long = Calendar.getInstance().apply {
        timeInMillis = startOfWeek()
        add(Calendar.DAY_OF_MONTH, 7)
    }.timeInMillis
    val productReportThisWeek : StateFlow<List<ProductReport>> = venteDao.getProductReportBetween(startOfWeek(), endOfWeek())
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val revenueThisWeekBounded: StateFlow<Double> = venteDao.getTotalRevenueBetween(startOfWeek(), endOfWeek())
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)


    /* Rapports par Mensuel  */
    private fun startOfMonth(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    private fun endOfMonth(): Long = Calendar.getInstance().apply {
        timeInMillis = startOfMonth()
        add(Calendar.MONTH, 1)
    }.timeInMillis

    val productReportThisMonth : StateFlow<List<ProductReport>> = venteDao.getProductReportBetween(startOfMonth(), endOfMonth())
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val revenueThisMonthBounded : StateFlow<Double> = venteDao.getTotalRevenueBetween(startOfMonth(), endOfMonth())
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)



    val weeklySales: StateFlow<List<SalesData>> =
        venteDao.getSalesSince(startOfWeek)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val monthlySales: StateFlow<List<SalesData>> =
        venteDao.getSalesByMonth()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val salesByCategory: StateFlow<List<SalesData>> =
        venteDao.getSalesByCategory()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val topProducts: StateFlow<List<ProductSale>> =
        venteDao.getTopSellingProducts()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- Totals ---

    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getStartOfMonth(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    val salesToday: StateFlow<Double> =
        venteDao.getTotalSalesSince(getStartOfDay())
            .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val salesThisWeek: StateFlow<Double> =
        venteDao.getTotalSalesSince(startOfWeek)
            .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val salesThisMonth: StateFlow<Double> =
        venteDao.getTotalSalesSince(getStartOfMonth())
            .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)


    companion object {
        fun provideFactory(
            venteDao: VenteDao
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(venteDao) as T
            }
        }
    }

    fun exportRapportPdf(
        context: Context,
        title: String,
        items: List<ProductReport>,
        total: Double,
    ) {
        // 1) Créer le doc PDF (A4 portrait)
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // ~A4 en points (72dpi)
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        // Styles
        val paintTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
            color = Color.BLACK
        }
        val paintSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
            color = Color.DKGRAY
        }
        val paintHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12f
            color = Color.BLACK
        }
        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
            color = Color.BLACK
        }
        val line = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date())
        var y = 40f
        val left = 32f
        val right = pageInfo.pageWidth - 32f

        // Titre + date
        canvas.drawText(title, left, y, paintTitle); y += 20f
        canvas.drawText("Généré le $dateStr", left, y, paintSub); y += 16f

        // Total
        canvas.drawText("Chiffre d'affaires : € ${formatMoney(total)}", left, y, paintHeader); y += 18f
        canvas.drawLine(left, y, right, y, line); y += 14f

        // En-têtes de colonnes
        val col1 = left
        val col2 = left + 180f
        val col3 = left + 280f
        val col4 = right - 80f
        canvas.drawText("Produit", col1, y, paintHeader)
        canvas.drawText("Quantité", col2, y, paintHeader)
        canvas.drawText("Montant", col3, y, paintHeader)
        canvas.drawText("Stock", col4, y, paintHeader)

        y += 14f
        canvas.drawLine(left, y, right, y, line); y += 10f

        // Lignes
        items.forEach { r ->
            // retour à la page si on déborde (simple: stop si plein)
            if (y > pageInfo.pageHeight - 40) return@forEach
            canvas.drawText(r.productName, col1, y, paintText)
            canvas.drawText("${r.totalQuantity}", col2, y, paintText)
            canvas.drawText("€ ${formatMoney(r.revenue)}", col3, y, paintText)
            canvas.drawText("${r.productStock}", col4, y, paintText)
            y += 16f
        }

        pdf.finishPage(page)

        // 2) Sauvegarder dans le cache
        val safeTitle = title.lowercase(Locale.ROOT).replace(" ", "_")
        val file = File(context.cacheDir, "rapport_${safeTitle}_${System.currentTimeMillis()}.pdf")
        pdf.writeTo(FileOutputStream(file))
        pdf.close()

        // 3) Partager via FileProvider
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val share = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title — généré le $dateStr")
        }
        context.startActivity(Intent.createChooser(share, "Partager le rapport PDF"))
    }

    private fun formatMoney(value: Double): String {
        return if (value % 1.0 == 0.0)
            "%,.0f".format(Locale.FRANCE, value)
        else
            "%,.2f".format(Locale.FRANCE, value)
    }

}