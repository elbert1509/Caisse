package com.example.caisse.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.Ticket
import com.example.caisse.data.VenteWithDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoriqueScreen(
    navController: NavController,
    menuViewModel: MenuViewModel
) {
    val invoicesWithDetails by menuViewModel.ventesTablesWithDetails.collectAsState()
    val ventesWithDetails by menuViewModel.ventesWithDetails.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historique des ventes") },
                actions = {
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                    }
                }
            )
        },
        bottomBar = {
            BottomHome(
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it },
                navController = navController
            )
        }
    ) { padding ->

        LaunchedEffect(Unit) {
            menuViewModel.loadVentesTablesHistory()
            menuViewModel.loadVentesHistory()
        }

        Column(modifier = Modifier.padding(padding).padding(12.dp)) {

            // Onglets pour basculer
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Tables") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Panier") }
                )
            }

            Spacer(Modifier.height(8.dp))

            if (selectedTab == 0) {
                LazyColumn {
                    items(invoicesWithDetails) { venteDetails ->
                        VenteCard(venteDetails, title = "Vente Table")
                    }
                }
            } else {
                LazyColumn {
                    items(ventesWithDetails) { venteDetails ->
                        VenteCard(venteDetails, title = "Vente Panier")
                    }
                }
            }
        }
    }
}



@Composable
fun VenteCard(venteDetails: VenteWithDetails, title: String = "Vente") {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(Date(venteDetails.vente.date))
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Date: $formattedDate", style = MaterialTheme.typography.bodySmall)
            Text("Total: ${venteDetails.vente.total} €",
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.titleMedium.fontSize
            )
            Spacer(Modifier.height(8.dp))
            venteDetails.lignes.forEach { ArticleRow(it) }
        }
    }
}
@Composable
fun ArticleRow(ticket: Ticket) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(ticket.produit.nom, style = MaterialTheme.typography.bodyMedium)
        Text("x${ticket.quantity} • ${String.format("%.2f €", ticket.produit.prix * ticket.quantity)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
