package com.example.caisse.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.caisse.data.HomeActionButton
import com.example.caisse.data.MenuViewModel

@Composable
fun AppNavigation() {

    val menuViewModel: MenuViewModel = viewModel(
        factory = MenuViewModel.provideFactory(context = LocalContext.current)
    )
    val navController = rememberNavController()


    NavHost(navController, startDestination = "home") {

        composable("home") { HomeScreen(
            onAction = { action ->
                when(action){
                    HomeActionButton.PRENDRE_COMMANDE -> navController.navigate("prendre_commande")
                    HomeActionButton.HISTORIQUE_COMMANDES ->  println("Historique des commandes pas encore implémenté")
                    HomeActionButton.PARTAGER_BOUTONS ->  println("Partage pas encore implémenté")
                    HomeActionButton.GERE_CATEGORIE -> navController.navigate("categorie")
                    HomeActionButton.GERER_INVENTAIRE -> navController.navigate("home")
                    HomeActionButton.EXPORTER ->  println("Inventaire pas encore implémenté")
                    HomeActionButton.GERER_PRODUITS ->  println("Inventaire pas encore implémenté")
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

