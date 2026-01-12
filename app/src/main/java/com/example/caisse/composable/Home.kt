package com.example.caisse.composable


import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableRestaurant
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.caisse.data.HomeActionButton
import com.example.caisse.data.HomeTileData
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.UserRole
import com.example.caisse.model.AuthViewModel

/**
 * Écran d’accueil PoS en Jetpack Compose
 * - Responsive grid (gère la rotation auto)
 * - Tuiles colorées avec icônes
 * - AppBar avec titre et bouton paramètres
 *
 * Utilisation :
 * setContent { HomeScreen(onAction = { action -> /* TODO */ }, onOpenSettings = { /* TODO */ }) }
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen( onAction: (HomeActionButton) -> Unit,
                navController: NavController,
                menuViewModel: MenuViewModel,
                authVm: AuthViewModel

) {

    val session by menuViewModel.sessionProfile.collectAsState()
    val role = session.role
    val name = menuViewModel.getVendeurNameById(session.vendeurId)

    val items = rememberHomeTiles(role)
    // Détecte l’orientation et la largeur pour fixer dynamiquement le nombre de colonnes
    val config = LocalConfiguration.current
    val orientation = config.orientation
    val screenWidthDp = config.screenWidthDp
    //val items = rememberHomeTiles()
    var selectedTab by remember { mutableIntStateOf(0) }
    val columns = when {
        // Très grands écrans ou tablette paysage -> 4 colonnes
        screenWidthDp >= 900 -> 4
        // Paysage sur téléphone -> 4 colonnes
        orientation == Configuration.ORIENTATION_LANDSCAPE -> 4
        // Portrait -> 2 colonnes
        else -> 2
    }

    val infos = menuViewModel.getInfos()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()

                            ) {
                                Text(text = "${infos?.name}" )
                                Text(text = "Vendeur: " + name, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }

                        },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("bluetooth") }) {
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

        LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(columns),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ){
            items(items){tile->
                ActionButton(
                    data = tile,
                    onClick = { onAction(tile.action) },
                    modifier = Modifier.padding(8.dp),
                    authVm = authVm,
                    navController = navController
                )

            }
        }

    }
}


@Composable
fun ActionButton(data: HomeTileData, onClick: () -> Unit, modifier: Modifier = Modifier, authVm: AuthViewModel, navController: NavController) {

    Card(
        modifier = modifier
            .clickable {
                onClick()
                authVm.enqueueSync(
                    context = navController.context,
                    tag = "sync"
                )
                       },
        colors = CardDefaults.cardColors(containerColor = data.color),
        elevation = cardElevation(6.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(data.icon, contentDescription = null, tint = White)
            Text(
                text = data.title,
                color = White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}






private fun defaultHomeButton(role: UserRole): List<HomeTileData> {
    val vendeurTiles = listOf(
        HomeTileData("Prendre Commande", Icons.Default.PointOfSale, color = Gray, HomeActionButton.PRENDRE_COMMANDE),
        HomeTileData("Historique", Icons.Default.History, color = Gray, HomeActionButton.HISTORIQUE_COMMANDES),
        HomeTileData("Table", Icons.Default.TableRestaurant, color = Gray, HomeActionButton.TABLE),
    )

    val adminPlus = listOf(
        HomeTileData("Exporter", Icons.Default.Share, color = Gray, HomeActionButton.EXPORTER),
        HomeTileData("Gestion", Icons.Default.Edit, color = Blue, HomeActionButton.GESTION),
    )

    return if (role == UserRole.VENDEUR) vendeurTiles else (vendeurTiles + adminPlus)
}

@Composable
private fun rememberHomeTiles(role: UserRole): List<HomeTileData> {
    return remember(role) { defaultHomeButton(role) }
}





@Preview
@Composable
fun HomeScreenPreview() {
    //HomeScreen( navController = NavController(LocalContext.current))
}



