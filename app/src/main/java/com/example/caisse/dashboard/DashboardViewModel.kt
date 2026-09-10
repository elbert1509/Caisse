package com.example.caisse.data

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
import com.example.caisse.model.SessionCaisseDao
import com.example.caisse.model.VendeurDao
import com.example.caisse.model.VenteDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale


class DashboardViewModel(
    venteDao: VenteDao,
    sessionCaisseDao: SessionCaisseDao,
    vendeurDao: VendeurDao
) : ViewModel() {

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



    // Le "jour" d'une vente est défini par la session de caisse (ouverture -> fermeture) dans
    // laquelle elle tombe, pas par le jour calendaire : une vente après minuit tant que la
    // caisse de la veille n'a pas été fermée compte pour la veille. On élargit la fenêtre de
    // requête d'un jour en amont pour couvrir une session ouverte juste avant startOfWeek, et on
    // ne garde ensuite que les jours métier appartenant réellement à la semaine affichée.
    private val startOfWeekDate: LocalDate =
        Instant.ofEpochMilli(startOfWeek).atZone(ZoneId.systemDefault()).toLocalDate()

    val weeklySales: StateFlow<List<SalesData>> =
        combine(
            venteDao.getVentesSince(startOfWeek - 24L * 60 * 60 * 1000),
            sessionCaisseDao.getSessionsSince(startOfWeek - 24L * 60 * 60 * 1000)
        ) { ventes, sessions ->
            bucketVentesParJourMetier(ventes, sessions)
                .filter { data ->
                    val jour = runCatching { LocalDate.parse(data.label) }.getOrNull()
                    jour != null && !jour.isBefore(startOfWeekDate) && jour.isBefore(startOfWeekDate.plusDays(7))
                }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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

    // Vue live du gérant : pour chaque vendeur, statut de sa caisse (ouverte/fermée), heures
    // d'ouverture/fermeture de sa dernière session, et total de ses ventes sur cette session.
    val suiviVendeurs: StateFlow<List<com.example.caisse.data.VendeurSuiviUi>> = combine(
        vendeurDao.getAllVendeur(),
        sessionCaisseDao.getSessionsSince(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000),
        venteDao.getAllVentes()
    ) { vendeurs, sessions, ventes ->
        vendeurs.map { vendeur ->
            val sessionsVendeur = sessions.filter { it.idVendeurOuverture == vendeur.id }
            val sessionOuverte = sessionsVendeur.firstOrNull { it.dateFermeture == null }
            val derniereSession = sessionsVendeur.maxByOrNull { it.dateOuverture }
            val sessionAffichee = sessionOuverte ?: derniereSession
            val totalSession = sessionAffichee?.let { s ->
                ventes.filter {
                    it.vendeurId == vendeur.id && !it.isDeleted &&
                        it.date >= s.dateOuverture && (s.dateFermeture == null || it.date < s.dateFermeture)
                }.sumOf { it.total }
            } ?: 0.0
            com.example.caisse.data.VendeurSuiviUi(
                vendeurId = vendeur.id,
                nom = "${vendeur.prenom} ${vendeur.nom}",
                ouverte = sessionOuverte != null,
                heureOuverture = sessionAffichee?.dateOuverture,
                heureFermeture = sessionAffichee?.dateFermeture,
                totalVentesSession = totalSession
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())


    companion object {
        fun provideFactory(
            venteDao: VenteDao,
            sessionCaisseDao: SessionCaisseDao,
            vendeurDao: VendeurDao
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(venteDao, sessionCaisseDao, vendeurDao) as T
            }
        }

        /**
         * Rattache chaque vente au jour métier de la session de caisse dans laquelle elle
         * tombe (dateOuverture <= vente.date < dateFermeture, ou "en cours" si la session est
         * encore ouverte). Une vente qui ne tombe dans aucune session connue (données
         * antérieures à l'introduction de cet historique, ou sans vendeur) retombe sur son
         * jour calendaire.
         *
         * Depuis qu'un vendeur peut avoir sa propre caisse, plusieurs sessions peuvent être
         * ouvertes EN MÊME TEMPS (un vendeur par caisse) : matcher uniquement sur la fenêtre de
         * dates sans tenir compte du vendeur attribuait les ventes du jour à la session encore
         * ouverte la plus ANCIENNE (première du tri par dateOuverture ASC), donc à un jour métier
         * périmé — les ventes du jour disparaissaient du graphe de la semaine. On ne considère
         * donc que les sessions ouvertes par LE MÊME vendeur que la vente.
         */
        private fun bucketVentesParJourMetier(
            ventes: List<Vente>,
            sessions: List<SessionCaisse>
        ): List<SalesData> {
            val now = System.currentTimeMillis()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val sessionsParVendeur = sessions.groupBy { it.idVendeurOuverture }
            val totaux = linkedMapOf<String, Double>()
            for (vente in ventes) {
                val session = vente.vendeurId
                    ?.let { sessionsParVendeur[it] }
                    ?.filter { s -> vente.date >= s.dateOuverture && vente.date < (s.dateFermeture ?: now) }
                    ?.maxByOrNull { it.dateOuverture }
                val jour = session?.jourMetier ?: sdf.format(Date(vente.date))
                totaux[jour] = (totaux[jour] ?: 0.0) + vente.total
            }
            return totaux.map { (label, amount) -> SalesData(label, amount) }.sortedBy { it.label }
        }
    }

    /**
     * Génère le rapport de la période (journalier / hebdo / mensuel) en PDF — en-tête boutique,
     * devise de la boutique, pagination automatique — puis ouvre le sélecteur de partage.
     */
    fun exportRapportPdf(
        context: Context,
        title: String,
        items: List<ProductReport>,
        total: Double,
        infos: com.example.caisse.data.ShopInfos? = null,
    ) {
        viewModelScope.launch {
            try {
                val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.caisse.util.PdfReportGenerator.generateRapportProduits(
                        destDir = context.cacheDir,
                        infos = infos,
                        title = title,
                        items = items,
                        total = total
                    )
                }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date())
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, "$title — généré le $dateStr")
                }
                context.startActivity(Intent.createChooser(share, "Partager le rapport PDF"))
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Échec de génération du rapport PDF", e)
                android.widget.Toast.makeText(
                    context, "Échec de génération du rapport PDF", android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

}