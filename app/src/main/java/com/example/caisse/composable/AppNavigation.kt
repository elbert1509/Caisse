package com.example.caisse.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
                    HomeActionButton.HISTORIQUE_COMMANDES ->  navController.navigate("historique")
                    HomeActionButton.PARTAGER_BOUTONS ->  println("Partage pas encore implémenté")
                    HomeActionButton.GERE_CATEGORIE -> navController.navigate("categorie")
                    HomeActionButton.GERER_INVENTAIRE -> navController.navigate("home")
                    HomeActionButton.EXPORTER ->  println("Inventaire pas encore implémenté")
                    HomeActionButton.GERER_PRODUITS -> navController.navigate("produit")
                    HomeActionButton.TABLE -> navController.navigate("table")
                    HomeActionButton.DONNES ->   navController.navigate("donnee")
                }
            },
            navController = navController,
        ) }
        composable("categorie") { CategoriesScreen( navController = navController, modifier = Modifier,
            viewModelcategories = menuViewModel
        ) }
        composable("produit") { ProductScreen( navController = navController, modifier = Modifier,
            viewModel = menuViewModel
        ) }
        composable("prendre_commande") { PrendreCommandeScreen(navController = navController, menuViewModel = menuViewModel) }
        composable("panier") { PanierScreen(navController = navController, menuViewModel = menuViewModel) }
        composable("table") { TableScreen(navController = navController, menuViewModel = menuViewModel) }
        composable(
            "table_details/{tableId}",
            arguments = listOf(navArgument("tableId") { type = NavType.StringType })
        ) { backStackEntry ->
            TableDetailsScreen(
                navController = navController,
                menuViewModel = menuViewModel,
                tableId = backStackEntry.arguments?.getString("tableId") ?: ""
            )
        }
        composable("historique") { HistoriqueScreen(navController = navController, menuViewModel = menuViewModel) }
        composable("donnee") { Donnee(navController = navController, menuViewModel = menuViewModel) }
    }

}


