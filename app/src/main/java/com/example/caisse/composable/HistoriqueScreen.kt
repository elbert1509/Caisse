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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.example.caisse.data.Produit
import com.example.caisse.data.Ticket
import com.example.caisse.data.VenteWithDetails
import com.example.caisse.session.CurrentUserViewModel
import com.example.caisse.session.Role
import com.example.caisse.util.formatPrice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoriqueScreen(
    navController: NavController,
    menuViewModel: MenuViewModel,
    currentUserViewModel: CurrentUserViewModel
) {
    val currentUser by currentUserViewModel.currentUser.collectAsState()
    val vendeurs by menuViewModel.vendeurs.collectAsState()
    val allInvoicesWithDetails by menuViewModel.ventesTablesWithDetails.collectAsState()
    val allVentesWithDetails by menuViewModel.ventesWithDetails.collectAsState()
    // Un vendeur ne voit que son propre historique ; le gérant voit tout.
    val invoicesWithDetails = remember(allInvoicesWithDetails, currentUser) {
        allInvoicesWithDetails.filter { currentUser?.role != Role.VENDEUR || it.vente.vendeurId == currentUser?.vendeur?.id }
    }
    val ventesWithDetails = remember(allVentesWithDetails, currentUser) {
        allVentesWithDetails.filter { currentUser?.role != Role.VENDEUR || it.vente.vendeurId == currentUser?.vendeur?.id }
    }
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
                    text = { Text("Comptoir") }
                )
            }

            Spacer(Modifier.height(8.dp))

            if (selectedTab == 0) {
                LazyColumn {
                    items(invoicesWithDetails) { venteDetails ->
                        val vendeurNom = vendeurs.find { it.id == venteDetails.vente.vendeurId }
                            ?.let { "${it.prenom} ${it.nom}" }
                        VenteCard(venteDetails, title = "Vente Table",devise = menuViewModel.getInfos()?.devise ?: "",
                            vendeurNom = vendeurNom,
                            onVenteClick = {
                                navController.navigate("ticket/${venteDetails.vente.id}")
                            }
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(ventesWithDetails) { venteDetails ->
                        VenteCard(
                            venteDetails,
                            title = "Comptoir",
                            devise = menuViewModel.getInfos()?.devise ?: "",
                            onVenteClick = {
                                navController.navigate("ticket/${venteDetails.vente.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun VenteCard(
    venteDetails: VenteWithDetails,
    title: String,
    devise: String,
    vendeurNom: String? = null,
    onVenteClick: () -> Unit = {}
) {
    val formattedDate = remember(venteDetails.vente.date) {
        SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault())
            .format(Date(venteDetails.vente.date))
    }

    val totalArticles = venteDetails.lignesActives.sumOf { it.ligne.quantity }
    val isTable = venteDetails.vente.tableId != null
    val previewLines = venteDetails.lignesActives.take(3)
    val hasMoreLines = venteDetails.lignesActives.size > 3

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = onVenteClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (vendeurNom != null) {
                        Text(
                            text = "Servie par $vendeurNom",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                    color = if (isTable) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isTable) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Total encaissé",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = formatPrice(venteDetails.vente.total, devise),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(10.dp))

            previewLines.forEach { ligne ->
                ArticlePreviewRow(
                    produitNom = ligne.produit.nom,
                    quantity = ligne.ligne.quantity,
                    total = ligne.ligne.sousTotal,
                    devise = devise
                )
            }

            if (hasMoreLines) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "+ ${venteDetails.lignesActives.size - 3} autre(s) article(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$totalArticles article(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Voir le ticket",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
@Composable
private fun ArticlePreviewRow(
    produitNom: String,
    quantity: Int,
    total: Double,
    devise: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = produitNom,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )

            Text(
                text = "Qté : $quantity",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = formatPrice(total, devise),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}