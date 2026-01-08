package com.example.caisse.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.caisse.data.MenuViewModel
import androidx.navigation.NavController
import androidx.room.util.getTotalChangedRows
import com.example.caisse.data.DashboardViewModel
import com.example.caisse.data.ProductReport
import com.example.caisse.ui.theme.MintEnd
import com.example.caisse.ui.theme.MintStart
import com.example.caisse.ui.theme.Slate100
import com.example.caisse.ui.theme.Slate500
import com.example.caisse.ui.theme.Slate700
import com.example.caisse.ui.theme.Slate900
import com.example.caisse.util.formatPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RapportData(navController: NavController, viewModel: MenuViewModel, dashboardViewModel: DashboardViewModel, initialTab: Int = 0){

    val todayList   by dashboardViewModel.productReportToday.collectAsState()
    val weekList    by dashboardViewModel.productReportThisWeek.collectAsState()
    val monthList   by dashboardViewModel.productReportThisMonth.collectAsState()
    val revToday    by dashboardViewModel.salesToday.collectAsState()
    val revWeek     by dashboardViewModel.salesThisWeek.collectAsState()
    val revMonth    by dashboardViewModel.salesThisMonth.collectAsState()
    val weeklySales by dashboardViewModel.weeklySales.collectAsState()
    val devise = viewModel.getInfos()






    val tabs = listOf("Journalier", "Hebdomadaire", "Mensuel")
    var selectedTab by remember { mutableStateOf(initialTab.coerceIn(0, 2)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Rapports de ventes", style = MaterialTheme.typography.titleLarge, color = Slate900)
                        Text("Détails par produit", style = MaterialTheme.typography.bodySmall, color = Slate500)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Bottom_rapport(
                selectedIndex = selectedTab,
                onExportClick = { tab ->
                    val (title, items, total) = when (tab) {
                        0 -> Triple("Rapport Journalier", todayList, revToday)
                        1 -> Triple("Rapport Hebdomadaire", weekList, revWeek)
                        else -> Triple("Rapport Mensuel", monthList, revMonth)
                    }
                    dashboardViewModel.exportRapportPdf(navController.context, title, items, total)
                }
            )
        },
        containerColor = Slate100
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White
            ){
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title)
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            when (selectedTab) {
                0 -> RapportCard("Aujourd'hui", total = revToday, items = todayList ,devise = devise?.devise ?: "")
                1 -> RapportCard("Cette semaine", total = revWeek, items = weekList, devise = devise?.devise ?: "")
                2 -> RapportCard("Ce mois-ci", total = revMonth, items = monthList, devise = devise?.devise ?: "")
            }
        }

    }

}


@Composable
private fun RapportCard(
    period : String,
    total : Double,
    items : List<ProductReport>,
    devise : String
){
    Card(
        elevation = cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Ventes $period", style = MaterialTheme.typography.titleMedium, color = Slate900)
            Spacer(Modifier.height(8.dp))
            TotalBar(total,devise)
            Spacer(Modifier.height(12.dp))

            if (items.isEmpty()) {
                EmptyState(
                    title = "Aucun rapport",
                    subtitle = "Aucun rapport disponible pour le moment."
                )
            }else {
                // Liste des produits vendus
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items) { p ->
                        ProductLine(p,devise)
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }

            }

        }

    }
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
@Composable
private fun TotalBar(total: Double,devise : String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color(0xFFF8FAFC), shape = MaterialTheme.shapes.medium)
            .padding(12.dp)
    ) {
        Column(Modifier.align(Alignment.CenterStart)) {
            Text("Chiffre d'affaires", style = MaterialTheme.typography.labelMedium, color = Slate500)
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatPrice(total,devise),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Slate900
            )
        }
        // Accent graphique façon Dashboard
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .height(6.dp)
                .fillMaxWidth(0.45f)
                .background(
                    Brush.linearGradient(listOf(MintStart, MintEnd)),
                    shape = MaterialTheme.shapes.medium
                )
        )
    }
}

@Composable
private fun ProductLine(p: ProductReport, devise: String) {
    // une petite card par ligne
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = cardElevation(2.dp),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(0.5f)) {
                Text(p.productName, style = MaterialTheme.typography.titleSmall, color = Slate900)
                Spacer(Modifier.height(2.dp))
                Text("${p.totalQuantity} vendu(s)", style = MaterialTheme.typography.bodySmall, color = Slate700)
            }
            Text(formatPrice(p.revenue,devise),Modifier.weight(0.1f), style = MaterialTheme.typography.titleSmall, color = Slate900)
            Column(Modifier.weight(0.4f)) {
                Text("Stock", style = MaterialTheme.typography.titleSmall, color = Slate900)
                Spacer(Modifier.height(2.dp))
                Text("${p.productStock} ", style = MaterialTheme.typography.bodySmall, color = Slate700)
            }
        }
    }
}

private fun Double.formatMoney(): String {
    val v = this
    return if (v % 1.0 == 0.0) "%,.0f".format(java.util.Locale.FRANCE, v)
    else "%,.2f".format(java.util.Locale.FRANCE, v)
}