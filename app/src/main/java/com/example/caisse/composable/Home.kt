package com.example.caisse.composable

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource // Import ajouté
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.caisse.R // Assurez-vous que cet import correspond à votre package
import com.example.caisse.data.HomeActionButton
import com.example.caisse.data.HomeTileData

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
fun HomeScreen(
    onAction: (HomeActionButton) -> Unit,
    navController: NavController
) {
    // Détecte l’orientation et la largeur pour fixer dynamiquement le nombre de colonnes
    val config = LocalConfiguration.current
    val orientation = config.orientation
    val screenWidthDp = config.screenWidthDp

    // Récupération des tuiles avec les textes issus de strings.xml
    val items = rememberHomeTiles()

    var selectedTab by remember { mutableIntStateOf(0) }
    val columns = when {
        // Très grands écrans ou tablette paysage -> 4 colonnes
        screenWidthDp >= 900 -> 4
        // Paysage sur téléphone -> 4 colonnes
        orientation == Configuration.ORIENTATION_LANDSCAPE -> 4
        // Portrait -> 2 colonnes
        else -> 2
    }
    Scaffold(
        topBar = {
            TopAppBar(
                // [MODIFICATION] Utilisation de stringResource
                title = { Text(text = stringResource(id = R.string.home_title)) },
                actions = {
                    IconButton(onClick = { navController.navigate("bluetooth") }) {
                        // [MODIFICATION] Ajout de la description pour l'accessibilité
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(id = R.string.settings_description)
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomHome(
                selectedIndex = selectedTab,
                onTabSelected = { },
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
            items(items){ tile ->
                ActionButton(data = tile, onClick = { onAction(tile.action) }, modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@Composable
fun ActionButton(data: HomeTileData, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable { onClick() },
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

/**
 * Génère la liste par défaut des boutons en utilisant les ressources strings.xml.
 */
@Composable
private fun getLocalizedHomeButtons(): List<HomeTileData> {
    return listOf(
        HomeTileData(stringResource(R.string.tile_prendre_commande), Icons.Default.PointOfSale, color = Gray, HomeActionButton.PRENDRE_COMMANDE),
        HomeTileData(stringResource(R.string.tile_historique), Icons.Default.History, color = Gray, HomeActionButton.HISTORIQUE_COMMANDES),
        HomeTileData(stringResource(R.string.tile_exporter), Icons.Default.Share, color = Gray, HomeActionButton.EXPORTER),
        HomeTileData(stringResource(R.string.tile_table), Icons.Default.TableRestaurant, color = Blue, HomeActionButton.TABLE),
        HomeTileData(stringResource(R.string.tile_gestion), Icons.Default.Edit, color = Blue, HomeActionButton.GESTION),
        HomeTileData(stringResource(R.string.tile_recherche), Icons.Default.Search, color = Blue, HomeActionButton.RECHERCHE)
    )
}

@Composable
private fun rememberHomeTiles(): List<HomeTileData> {
    // On récupère la liste traduite
    val defaultTiles = getLocalizedHomeButtons()

    // On passe cette liste par défaut au Saver pour qu'il puisse restaurer les éléments manquants
    return rememberSaveable(saver = homeTilesSaver(defaultTiles)) {
        defaultTiles
    }
}

/**
 * Le Saver est maintenant une fonction qui prend la liste par défaut en paramètre
 * pour pouvoir reconstruire les objets correctement lors de la restauration.
 */
private fun homeTilesSaver(defaultList: List<HomeTileData>): Saver<List<HomeTileData>, Any> = Saver(
    save = { list -> list.map { it.title } }, // on sauvegarde juste les titres (ordre)
    restore = { saved ->
        val order = (saved as List<*>).filterIsInstance<String>()
        val byTitle = defaultList.associateBy { it.title }

        // Recompose la liste dans l’ordre sauvegardé, puis ajoute les manquants
        (order.mapNotNull { byTitle[it] } + defaultList.filter { it.title !in order })
    }
)

@Preview
@Composable
fun HomeScreenPreview() {
    // HomeScreen( navController = NavController(LocalContext.current))
}