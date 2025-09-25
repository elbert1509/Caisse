package com.example.caisse.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.caisse.data.HomeActionButton
import com.example.caisse.data.MenuViewModel

@Composable
fun AppNavigation() {

    val navController = rememberNavController()
    val menuViewModel: MenuViewModel = viewModel()

    NavHost(navController, startDestination = "home") {

        composable("home") { HomeScreen(
            onAction = { action ->
                when(action){
                    HomeActionButton.PRENDRE_COMMANDE -> navController.navigate("prendre_commande")
                    HomeActionButton.HISTORIQUE_COMMANDES -> TODO()
                    HomeActionButton.PARTAGER_BOUTONS -> TODO()
                    HomeActionButton.GERE_CATEGORIE -> navController.navigate("categorie")
                    HomeActionButton.GERER_INVENTAIRE -> navController.navigate("home")
                    HomeActionButton.EXPORTER -> TODO()
                    HomeActionButton.GERER_PRODUITS -> TODO()
                }
            },
            navController = navController,
        ) }
        composable("categorie") { CategoriesScreen( navController = navController, modifier = Modifier,
            viewModelcategories = menuViewModel
        ) }
        composable("prendre_commande") { PrendreCommandeScreen(navController = navController, menuViewModel = menuViewModel) }
    }

}

