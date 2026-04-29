package com.example.caisse.composable

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.caisse.data.HomeActionButton
import com.example.caisse.data.HomeTileData
import com.example.caisse.data.MenuViewModel
import com.example.caisse.ui.theme.Accent500
import com.example.caisse.ui.theme.Brand600
import com.example.caisse.ui.theme.SemanticGreen
import com.example.caisse.ui.theme.SemanticOrange
import com.example.caisse.ui.theme.Slate700
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAction: (HomeActionButton) -> Unit,
    navController: NavController,
    viewModel: MenuViewModel
) {
    val config = LocalConfiguration.current
    val orientation = config.orientation
    val screenWidthDp = config.screenWidthDp
    val items = rememberHomeTiles()
    var selectedTab by remember { mutableIntStateOf(0) }
    val columns = when {
        screenWidthDp >= 900 -> 4
        orientation == Configuration.ORIENTATION_LANDSCAPE -> 4
        else -> 2
    }
    val caisseOuverte by viewModel.caisseOuverte.collectAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "MaCaissePro",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (caisseOuverte) "Session ouverte" else "Session fermée",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
                actions = {
                    IconButton(onClick = { navController.navigate("bluetooth") }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Paramètres")
                    }
                }
            )
        },
        bottomBar = {
            if (caisseOuverte) {
                BottomHome(
                    selectedIndex = selectedTab,
                    onTabSelected = { selectedTab = it },
                    navController = navController
                )
            }
        }
    ) { padding ->

        if (!caisseOuverte) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.Store,
                    contentDescription = null,
                    modifier = Modifier.size(88.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Caisse fermée",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Ouvrez la session pour commencer à encaisser",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { viewModel.ouvrirLaCaisse(UUID.randomUUID()) },
                    modifier = Modifier.height(52.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(
                        "Ouvrir la caisse",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(columns),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { tile ->
                    ActionButton(
                        data = tile,
                        onClick = { onAction(tile.action) }
                    )
                }
            }
        }
    }
}

@Composable
fun ActionButton(data: HomeTileData, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = data.color),
        elevation = cardElevation(4.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                data.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = data.title,
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun defaultHomeButton(): List<HomeTileData> = listOf(
    HomeTileData("Prendre Commande", Icons.Default.PointOfSale,     color = Brand600,       HomeActionButton.PRENDRE_COMMANDE),
    HomeTileData("Historique",        Icons.Default.History,          color = Accent500,      HomeActionButton.HISTORIQUE_COMMANDES),
    HomeTileData("Exporter",          Icons.Default.Share,            color = SemanticGreen,  HomeActionButton.EXPORTER),
    HomeTileData("Table",             Icons.Default.TableRestaurant,  color = SemanticOrange, HomeActionButton.TABLE),
    HomeTileData("Gestion",           Icons.Default.Edit,             color = Slate700,       HomeActionButton.GESTION),
)

@Composable
private fun rememberHomeTiles(): List<HomeTileData> {
    return rememberSaveable(saver = HomeTilesSaver) {
        defaultHomeButton()
    }
}

private val HomeTilesSaver: Saver<List<HomeTileData>, Any> = Saver(
    save = { list -> list.map { it.title } },
    restore = { saved ->
        val order = (saved as List<*>).filterIsInstance<String>()
        val byTitle = defaultHomeButton().associateBy { it.title }
        (order.mapNotNull { byTitle[it] } + defaultHomeButton().filter { it.title !in order })
    }
)

@Preview
@Composable
fun HomeScreenPreview() {
    // HomeScreen(navController = ..., viewModel = ...)
}
