package com.example.caisse.composable

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.caisse.kiosk.AdminExitDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.caisse.data.DashboardViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import androidx.compose.ui.viewinterop.AndroidView
import com.example.caisse.ui.theme.Indigo
import com.example.caisse.ui.theme.MintEnd
import com.example.caisse.ui.theme.MintStart
import com.example.caisse.ui.theme.Slate100
import com.example.caisse.ui.theme.Slate500
import com.example.caisse.ui.theme.Slate700
import com.example.caisse.ui.theme.Slate900
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import androidx.core.graphics.toColorInt
import com.example.caisse.data.MenuViewModel
import com.example.caisse.util.formatPrice

/* ------------------------------- Screen root ------------------------------- */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(navController: NavController, viewModel: DashboardViewModel, menuViewModel: MenuViewModel) {
    var showAdminDialog by remember { mutableStateOf(false) }
    if (showAdminDialog) {
        AdminExitDialog(onDismiss = { showAdminDialog = false })
    }
    val weeklySales     by viewModel.weeklySales.collectAsState()
    val monthlySales    by viewModel.monthlySales.collectAsState()
    val salesByCategory by viewModel.salesByCategory.collectAsState()
    val topProducts     by viewModel.topProducts.collectAsState()
    val salesToday      by viewModel.salesToday.collectAsState()
    val salesThisWeek   by viewModel.salesThisWeek.collectAsState()
    val salesThisMonth  by viewModel.salesThisMonth.collectAsState()
    val shopInfos = menuViewModel.getInfos()

    Log.d("DashboardScreen", "salesByCategory: ${salesByCategory.size}")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                            onLongClick = { showAdminDialog = true } // appui long = accès admin (kiosk)
                        )
                    ) {
                        Text("Tableau de bord", style = MaterialTheme.typography.titleLarge, color = Slate900)
                        Text("Aperçu des ventes", style = MaterialTheme.typography.bodySmall, color = Slate500)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Slate100
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            /* --- KPI Row (Today / Week / Month) --- */
            item {
                KpiRow(today = salesToday, week = salesThisWeek, month = salesThisMonth, navController = navController, devise = shopInfos?.devise ?: "€" )
            }

            /* --- Weekly line (cubic + dégradé + labels jours) --- */
            item {
                ChartCard("Ventes de la semaine") {
                    AndroidView(
                        factory = { context -> LineChart(context) },
                        modifier = Modifier.fillMaxWidth().height(280.dp),
                        update = { chart ->
                            val dayLabels = listOf("Lun","Mar","Mer","Jeu","Ven","Sam","Dim")
                            val amounts = FloatArray(7) { 0f }
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

                            weeklySales.forEach { data ->
                                try {
                                    val d = sdf.parse(data.label)
                                    val cal = java.util.Calendar.getInstance().apply { time = d!! }
                                    val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK) // 1..7 (Sun=1)
                                    val idx = ((dayOfWeek + 5) % 7) // Mon=0 … Sun=6
                                    amounts[idx] += data.amount.toFloat()
                                } catch (_: Exception) {}
                            }
                            val entries = amounts.mapIndexed { i, v -> Entry(i.toFloat(), v) }
                            val dataSet = LineDataSet(entries, "").apply {
                                mode = LineDataSet.Mode.CUBIC_BEZIER
                                lineWidth = 2.6f
                                setDrawCircles(true)
                                circleRadius = 4.5f
                                setCircleColor("#10B981".toColorInt())
                                color = "#10B981".toColorInt()
                                setDrawValues(true)
                                valueTextSize = 10f
                                valueTextColor = "#0F172A".toColorInt()
                                setDrawFilled(true)
                                valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                                    override fun getPointLabel(entry: Entry?): String =
                                        if (entry != null && entry.y > 0f)  formatPrice(entry.y.toDouble(),shopInfos?.devise ?: "€")  else ""
                                }
                                val grad = android.graphics.drawable.GradientDrawable(
                                    android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                                    intArrayOf(
                                        "#3310B981".toColorInt(),
                                        "#0010B981".toColorInt()
                                    )
                                )
                                fillDrawable = grad
                            }
                            chart.apply {
                                data = LineData(dataSet)
                                xAxis.apply {
                                    position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                                    valueFormatter = IndexAxisValueFormatter(dayLabels)
                                    granularity = 1f
                                    setLabelCount(7, true)
                                    textColor = "#64748B".toColorInt()
                                    setDrawGridLines(false)
                                }
                                axisLeft.apply {
                                    axisMinimum = 0f
                                    setDrawGridLines(true)
                                    gridColor = "#E2E8F0".toColorInt()
                                    textColor = "#64748B".toColorInt()
                                }
                                axisRight.isEnabled = false
                                description.isEnabled = false
                                legend.isEnabled = false
                                setTouchEnabled(true)
                                setPinchZoom(true)
                                setExtraOffsets(8f, 8f, 8f, 12f)
                                animateX(800)
                                invalidate()
                            }
                        }
                    )
                }
            }

            /* --- Monthly bars (labels FR + style propre) --- */
            item {
                ChartCard("Ventes par mois") {
                    AndroidView(
                        factory = { context -> BarChart(context) },
                        modifier = Modifier.fillMaxWidth().height(280.dp),
                        update = { chart ->
                            val entries = monthlySales.mapIndexed { index, data ->
                                BarEntry(index.toFloat(), data.amount.toFloat())
                            }
                            val dataSet = BarDataSet(entries, "").apply {
                                color = "#3B82F6".toColorInt()
                                valueTextColor = "#0F172A".toColorInt()
                                valueTextSize = 9f
                                setDrawValues(true)
                                highLightColor = "#3B82F6".toColorInt()
                            }
                            chart.data = BarData(dataSet).apply { barWidth = 0.5f }

                            val labels = monthlySales.map { sales ->
                                val month = sales.label.substring(5, 7).toInt()
                                listOf("Janv","Fév","Mars","Avr","Mai","Juin","Juil","Août","Sept","Oct","Nov","Déc")[month - 1]
                            }

                            chart.apply {
                                xAxis.apply {
                                    valueFormatter = IndexAxisValueFormatter(labels)
                                    position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                                    granularity = 1f
                                    textColor = "#64748B".toColorInt()
                                    setDrawGridLines(false)
                                }
                                axisLeft.apply {
                                    axisMinimum = 0f
                                    textColor = "#64748B".toColorInt()
                                    gridColor = "#E2E8F0".toColorInt()
                                }
                                axisRight.isEnabled = false
                                legend.isEnabled = false
                                description.isEnabled = false
                                setFitBars(true)
                                animateY(800)
                                invalidate()
                            }
                        }
                    )
                }
            }

            /* --- Donut par catégories --- */
            item {
                ChartCard("Répartition par catégorie") {
                    if (salesByCategory.isEmpty()) {
                        EmptyState(
                            title = "Aucune donnée",
                            subtitle = "Fais une première vente pour voir la répartition par catégories."
                        )
                    } else {
                        AndroidView(
                            factory = { context -> PieChart(context) },
                            modifier = Modifier.fillMaxWidth().height(280.dp),
                            update = { chart ->
                                chart.clear()
                                chart.setNoDataText("Aucune donnée à afficher")
                                chart.setNoDataTextColor("#64748B".toColorInt())

                                val entries = salesByCategory.map { PieEntry(it.amount.toFloat(), it.label) }
                                val set = PieDataSet(entries, "").apply {
                                    colors = listOf(
                                        "#10B981".toColorInt(),
                                        "#3B82F6".toColorInt(),
                                        "#F59E0B".toColorInt(),
                                        "#EF4444".toColorInt(),
                                        "#8B5CF6".toColorInt(),
                                        "#14B8A6".toColorInt()
                                    )
                                    sliceSpace = 2f
                                    valueTextColor = "#0F172A".toColorInt()
                                    valueTextSize = 10f
                                }
                                chart.apply {
                                    data = PieData(set)
                                    isDrawHoleEnabled = true
                                    holeRadius = 60f
                                    transparentCircleRadius = 65f
                                    setUsePercentValues(false)
                                    centerText = "Catégories"
                                    setCenterTextColor("#334155".toColorInt())
                                    setCenterTextSize(14f)
                                    legend.isEnabled = false
                                    description.isEnabled = false
                                    setEntryLabelColor("#334155".toColorInt())
                                    setEntryLabelTextSize(11f)
                                    animateY(800)
                                    invalidate()
                                }
                            }
                        )
                    }
                }
            }

            /* --- Top produits (barres) --- */
            item {
                ChartCard("Meilleurs produits") {
                    AndroidView(
                        factory = { context -> BarChart(context) },
                        modifier = Modifier.fillMaxWidth().height(280.dp),
                        update = { chart ->
                            val entries = topProducts.mapIndexed { index, p -> BarEntry(index.toFloat(), p.totalQuantity.toFloat()) }
                            val labels = topProducts.map { it.productName }
                            val set = BarDataSet(entries, "").apply {
                                color = "#10B981".toColorInt()
                                valueTextColor = "#0F172A".toColorInt()
                                valueTextSize = 9f
                            }
                            chart.apply {
                                data = BarData(set).apply { barWidth = 0.6f }
                                xAxis.apply {
                                    valueFormatter = IndexAxisValueFormatter(labels)
                                    position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                                    granularity = 1f
                                    textColor = "#64748B".toColorInt()
                                    setDrawGridLines(false)
                                    labelRotationAngle = -25f // lisible même avec beaucoup d’items
                                }
                                axisLeft.apply {
                                    axisMinimum = 0f
                                    textColor = "#64748B".toColorInt()
                                    gridColor = "#E2E8F0".toColorInt()
                                }
                                axisRight.isEnabled = false
                                legend.isEnabled = false
                                description.isEnabled = false
                                animateY(750)
                                invalidate()
                            }
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

/* ------------------------------ Components ------------------------------ */

/** Carte blanche réutilisable pour les graphiques : titre + contenu, style unifié. */
@Composable
private fun ChartCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        elevation = cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Slate900)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun KpiRow(today: Double, week: Double, month: Double,navController: NavController, devise : String ) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        KpiCard(
            title = "Aujourd'hui",
            amount = today,
            gradient = Brush.linearGradient(listOf(MintStart, MintEnd)),
            modifier = Modifier.weight(1f),
            onClick = { navController.navigate("rapport/0") },
            devise = devise

        )
        KpiCard(
            title = "Cette semaine",
            amount = week,
            gradient = Brush.linearGradient(listOf(Indigo, Color(0xFF2563EB))),
            modifier = Modifier.weight(1f),
            onClick = { navController.navigate("rapport/1") },
            devise = devise
        )
        KpiCard(
            title = "Ce mois",
            amount = month,
            gradient = Brush.linearGradient(listOf(Slate700, Slate900)),
            modifier = Modifier.weight(1f),
            onClick = { navController.navigate("rapport/2") },
            devise = devise
        )
    }
}

@Composable
private fun KpiCard(
    title: String,
    amount: Double,
    gradient: Brush,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    devise : String
) {
    Card(
        modifier = modifier.height(110.dp),
        elevation = cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = MaterialTheme.shapes.large,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .padding(14.dp)
        ) {
            Column(Modifier.align(Alignment.TopStart)) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = Slate500)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = formatPrice(amount,devise),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Slate900
                )
            }
            // Accent “banking” en bas
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .height(6.dp)
                    .fillMaxWidth(0.55f)
                    .background(gradient, shape = MaterialTheme.shapes.medium)
            )
        }
    }
}

/* ------------------------------ Utils ------------------------------ */

private fun Double.formatMoney(): String {
    val v = this
    return if (v % 1.0 == 0.0) "%,.0f".format(java.util.Locale.FRANCE, v)
    else "%,.2f".format(java.util.Locale.FRANCE, v)
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(Color(0xFFF8FAFC)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = Slate700, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = Slate500, style = MaterialTheme.typography.bodySmall)
    }
}