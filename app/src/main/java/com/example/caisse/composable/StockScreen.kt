package com.example.piece.composable

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.piece.data.MenuViewModel
import com.example.piece.data.Produit
import com.example.piece.ui.theme.Indigo
import com.example.piece.ui.theme.MintEnd
import com.example.piece.ui.theme.MintStart
import com.example.piece.ui.theme.Slate100
import com.example.piece.ui.theme.Slate500
import com.example.piece.ui.theme.Slate700
import com.example.piece.ui.theme.Slate900
import com.example.piece.util.formatPrice
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(navController: NavController, viewModel: MenuViewModel) {

    val produits by viewModel.produits.collectAsState() // Liste des produits (prix, stock, etc.)
    val lignes = remember(produits) {
        produits
            .filter { it.isActive } // on n’affiche que les produits actifs
            .map { it.toUiRow() }
            .sortedByDescending { it.revenue }
    }

    val totalUnits = remember(lignes) { lignes.sumOf { it.stock } }
    val totalRevenue = remember(lignes) { lignes.sumOf { it.revenue } }
    val maxRevenue = remember(lignes) { lignes.maxOfOrNull { it.revenue } ?: 0.0 }
    val shopInfos = viewModel.getInfos()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Stock produits", style = MaterialTheme.typography.titleLarge, color = Slate900)
                        Text("Inventaire & CA potentiel", style = MaterialTheme.typography.bodySmall, color = Slate500)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Slate100,
        bottomBar = {
            BottomTotalBar(total = totalRevenue, devise = shopInfos?.devise ?: "")
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // KPIs (compte produits, unités en stock, CA potentiel)
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Produits actifs",
                        value = lignes.size.toDouble(),
                        gradient = Brush.linearGradient(listOf(MintStart, MintEnd)),
                        modifier = Modifier.weight(1f),
                        isMoney = false,
                        devise = shopInfos?.devise ?: ""
                    )
                    KpiCard(
                        title = "Unités en stock",
                        value = totalUnits.toDouble(),
                        gradient = Brush.linearGradient(listOf(Indigo, Color(0xFF2563EB))),
                        modifier = Modifier.weight(1f),
                        isMoney = false,
                        devise = shopInfos?.devise ?: ""
                    )
                    KpiCard(
                        title = "CA potentiel",
                        value = totalRevenue,
                        gradient = Brush.linearGradient(listOf(Slate700, Slate900)),
                        modifier = Modifier.weight(1f),
                        devise = shopInfos?.devise ?: ""
                    )
                }
            }

            // Liste Stock par produit
            item {
                Card(
                    elevation = cardElevation(6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "Stock par produit",
                            style = MaterialTheme.typography.titleMedium,
                            color = Slate900
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "CA potentiel = stock × prix unitaire",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                        Spacer(Modifier.height(12.dp))

                        if (lignes.isEmpty()) {
                            Text(
                                "Aucun produit actif ou pas de données.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate500
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                lignes.forEach { row ->
                                    StockRow(
                                        name = row.name,
                                        price = row.price,
                                        stock = row.stock,
                                        revenue = row.revenue,
                                        ratio = if (maxRevenue > 0.0) (row.revenue / maxRevenue).toFloat() else 0f,
                                        devise = shopInfos?.devise ?: ""
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(72.dp)) } // pour que la bottomBar ne masque pas la liste
        }
    }
}

/* ------------------------------ UI components ------------------------------ */

@Composable
private fun BottomTotalBar(total: Double, devise: String ) {
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Chiffre d'affaires total potentiel", color = Slate500, style = MaterialTheme.typography.bodyMedium)
            Text(formatPrice(total,devise), color = Slate900, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: Double,
    gradient: Brush,
    modifier: Modifier = Modifier,
    isMoney: Boolean = true,
    devise: String
) {
    Card(
        modifier = modifier.height(110.dp),
        elevation = cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = MaterialTheme.shapes.large
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
                    text = if (isMoney) formatPrice( value,devise) else value.formatNumber(),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Slate900
                )
            }
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

@Composable
private fun StockRow(
    name: String,
    price: Double,
    stock: Int,
    revenue: Double,
    ratio: Float,
    devise: String
) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall, color = Slate900)
                Text(formatPrice(price, devise = devise) +" • stock: $stock", style = MaterialTheme.typography.bodySmall, color = Slate500)
            }
            Text( formatPrice(revenue,devise) , style = MaterialTheme.typography.titleSmall, color = Slate900)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { ratio.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
        )
    }
}

/* ------------------------------ Mapping & utils ------------------------------ */

private data class UiRow(
    val name: String,
    val price: Double,
    val stock: Int,
    val revenue: Double
)

private fun Produit.toUiRow(): UiRow =
    UiRow(
        name = nom,
        price = prix,
        stock = max(0, stock),
        revenue = prix * max(0, stock)
    )


private fun Double.formatNumber(): String {
    val v = this
    return if (v % 1.0 == 0.0) "%,.0f".format(java.util.Locale.FRANCE, v)
    else "%,.1f".format(java.util.Locale.FRANCE, v)
}
