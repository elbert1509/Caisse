package com.example.oudeika.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.oudeika.bluetooth.BluetoothViewModel
import com.example.oudeika.bluetooth.ParametreBluetooothScreen
import com.example.oudeika.data.DashboardViewModel
import com.example.oudeika.data.HomeActionButton
import com.example.oudeika.data.MenuViewModel
import com.example.oudeika.model.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavigation() {

    val menuViewModel: MenuViewModel = viewModel(
        factory = MenuViewModel.provideFactory(context = LocalContext.current)
    )
    val bluetoothViewModel: BluetoothViewModel = viewModel(
        factory = BluetoothViewModel.provideFactory()
    )
    val navController = rememberNavController()
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.provideFactory(venteDao = menuViewModel.repository.venteDao)
    )
    val authViewModel = remember { AuthViewModel() }
    val isSignedIn = remember { FirebaseAuth.getInstance().currentUser != null }


    NavHost(navController,
        startDestination = if (isSignedIn) "home" else "login"
    ) {
        composable("home") { HomeScreen(
            onAction = { action ->
                when(action){
                    HomeActionButton.PRENDRE_COMMANDE -> navController.navigate("prendre_commande")
                    HomeActionButton.HISTORIQUE_COMMANDES ->  navController.navigate("historique")
                    HomeActionButton.PARTAGER_BOUTONS ->  println("Partage pas encore implémenté")
                    HomeActionButton.GERE_CATEGORIE -> navController.navigate("categorie")
                    HomeActionButton.GERER_INVENTAIRE -> navController.navigate("inventaire")
                    HomeActionButton.EXPORTER ->  navController.navigate("rapport")
                    HomeActionButton.GERER_PRODUITS -> navController.navigate("produit")
                    HomeActionButton.TABLE -> navController.navigate("table")
                    HomeActionButton.DONNES ->   navController.navigate("donnee")
                    HomeActionButton.DASHBOARD -> navController.navigate("dashboard")
                    HomeActionButton.STOCK ->  navController.navigate("stock")
                    HomeActionButton.GESTION ->  navController.navigate("gestion")
                    HomeActionButton.RECHERCHE ->  navController.navigate("recherche")

                }
            },
            navController = navController,
        ) }
        composable("categorie") { CategoriesScreen( navController = navController, modifier = Modifier,
            viewModelcategories = menuViewModel
        ) }
        composable("produit") { ProductScreen(  modifier = Modifier,
            viewModel = menuViewModel
        ) }
        composable("prendre_commande") { PrendreCommandeScreen(navController = navController, menuViewModel = menuViewModel, bluetoothViewModel = bluetoothViewModel) }
        composable("panier") { PanierScreen(navController = navController, menuViewModel = menuViewModel, authVm = authViewModel) }
        composable("table") { TableScreen(navController = navController, menuViewModel = menuViewModel, authViewModel = authViewModel) }
        composable(
            "table_details/{tableId}",
            arguments = listOf(navArgument("tableId") { type = NavType.StringType })
        ) { backStackEntry ->
            TableDetailsScreen(
                navController = navController,
                menuViewModel = menuViewModel,
                tableId = backStackEntry.arguments?.getString("tableId") ?: "",
                bluetoothViewModel = bluetoothViewModel,
                authVm = authViewModel
            )
        }
        composable("historique") { HistoriqueScreen(navController = navController, menuViewModel = menuViewModel) }
        composable("donnee") { Donnee(navController = navController, menuViewModel = menuViewModel) }
        composable("bluetooth") { ParametreBluetooothScreen(navController = navController, viewModel = bluetoothViewModel, authVm = authViewModel,menuViewModel = menuViewModel) }
        composable("dashboard") { DashboardScreen(navController = navController, viewModel = dashboardViewModel, menuViewModel = menuViewModel) }
        composable("stock") { StockScreen(navController = navController, viewModel = menuViewModel) }
        composable("inventaire") { InventaireScreen(navController = navController, viewModel = menuViewModel) }
        composable("rapport") { RapportData(navController = navController, viewModel = menuViewModel, dashboardViewModel = dashboardViewModel)}
        composable(
            route = "rapport/{tabIndex}",
            arguments = listOf(navArgument("tabIndex") { type = NavType.IntType; defaultValue = 0 })
        ) { backStackEntry ->
            val tab = backStackEntry.arguments?.getInt("tabIndex") ?: 0
            RapportData(
                navController = navController,
                viewModel = menuViewModel,
                dashboardViewModel = dashboardViewModel,
                initialTab = tab
            )
        }
        composable("login") {
            val authVm = remember { AuthViewModel() } // ou via hiltViewModel() si tu utilises Hilt
            LoginScreen(navController = navController, vm = authVm, onSignedInNavigateRoute = "home", menuViewModel = menuViewModel)
        }
        composable("infos") { InfosScreen(navController = navController, viewModel = menuViewModel) }
        composable("gestion") { GestionScreen(navController = navController, viewModel = menuViewModel) }
        composable("vente") { VenteScreen(navController = navController, viewModel = menuViewModel) }
        composable("recherche") { RechercheScreen( viewModel = menuViewModel) }

    }

}


