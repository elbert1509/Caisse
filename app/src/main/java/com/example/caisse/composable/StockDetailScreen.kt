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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.CaisseRepository.JourStockDetail
import com.example.caisse.ui.theme.Brand600
import com.example.caisse.ui.theme.SemanticGreen
import com.example.caisse.ui.theme.SemanticRed
import com.example.caisse.ui.theme.Slate100
import com.example.caisse.ui.theme.Slate500
import com.example.caisse.ui.theme.Slate900
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Détail du stock d'un produit sur les 7 derniers jours (Stock / Ajout / Vente / Reste par
 * jour). Accessible uniquement depuis l'écran Stock de Gestion (clic sur une ligne produit) —
 * pas depuis la vue Stock simplifiée de l'Accueil.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    navController: NavController,
    viewModel: MenuViewModel,
    produitId: UUID
) {
    val produits by viewModel.produits.collectAsState()
    val produitNom = produits.find { it.id == produitId }?.nom ?: "Produit"

    var jours by remember { mutableStateOf<List<JourStockDetail>?>(null) }

    LaunchedEffect(produitId) {
        jours = viewModel.getDetailStockProduit(produitId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(produitNom, style = MaterialTheme.typography.titleLarge, color = Slate900)
                        Text(
                            "Historique stock — 7 derniers jours",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Slate100
    ) { padding ->
        val joursValue = jours
        if (joursValue == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(joursValue) { index, jour ->
                    JourStockCard(index = index, jour = jour)
                }
            }
        }
    }
}

@Composable
private fun JourStockCard(index: Int, jour: JourStockDetail) {
    val label = when (index) {
        0 -> "Aujourd'hui"
        1 -> "Hier"
        else -> "Il y a $index jours"
    }
    val dateFormatee = remember(jour.dateDebutJour) {
        SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(jour.dateDebutJour))
    }

    Column {
        Box(
            modifier = Modifier
                .background(Brand600, shape = RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                "$label • $dateFormatee",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Spacer(Modifier.height(0.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = cardElevation(4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                StatLine("Stock début de journée", jour.stockDebut.toString(), Slate900)
                Spacer(Modifier.height(10.dp))
                StatLine("Ajout", "+${jour.ajout}", SemanticGreen)
                Spacer(Modifier.height(6.dp))
                StatLine("Vente", "-${jour.vente}", SemanticRed)
                if (jour.ajustementNegatif > 0) {
                    Spacer(Modifier.height(6.dp))
                    StatLine("Ajustement (perte/correction)", "-${jour.ajustementNegatif}", SemanticRed)
                }
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.HorizontalDivider()
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Reste", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Slate900)
                    Text(
                        jour.reste.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                }
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Slate500)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
