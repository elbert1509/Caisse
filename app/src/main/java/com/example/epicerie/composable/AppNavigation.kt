package com.example.epicerie.composable

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
import com.example.epicerie.bluetooth.BluetoothViewModel
import com.example.epicerie.bluetooth.ParametreBluetooothScreen
import com.example.epicerie.data.DashboardViewModel
import com.example.epicerie.data.HomeActionButton
import com.example.epicerie.data.MenuViewModel
import com.example.epicerie.model.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavigation() {

    val menuViewModel: MenuViewModel = viewModel(
        factory = MenuViewModel.provideFactory(context = LocalContext.current)
    )
    val bluetoothViewModel: BluetoothViewModel = viewModel(
        factory = BluetoothViewModel.provideFactory(
            application = LocalContext.current.applicationContext as android.app.Application
        )
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
                    HomeActionButton.STOCK ->  navController.navigate("stock?showRevenue=false")
                    HomeActionButton.GESTION ->  navController.navigate("gestion")
                    HomeActionButton.RECHERCHE ->  navController.navigate("recherche")
                    HomeActionButton.VOITURE ->  navController.navigate("voiture_detail")
                    HomeActionButton.CLOTURE ->  navController.navigate("cloture")

                }
            },
            navController = navController,
            menuViewModel = menuViewModel,
            authVm = authViewModel
        ) }
        composable("categorie") { CategoriesScreen( navController = navController, modifier = Modifier,
            viewModelcategories = menuViewModel
        ) }
        composable("produit") { ProductScreen(  modifier = Modifier,
            viewModel = menuViewModel,
            navController = navController
        ) }
        composable("prendre_commande") { PrendreCommandeScreen(navController = navController, menuViewModel = menuViewModel, bluetoothViewModel = bluetoothViewModel) }
        composable("panier") { PanierScreen(navController = navController, menuViewModel = menuViewModel, authVm = authViewModel, bluetoothViewModel = bluetoothViewModel) }
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
        composable("historique") { HistoriqueScreen(navController = navController, menuViewModel = menuViewModel, bluetoothViewModel = bluetoothViewModel) }
        composable("donnee") { Donnee( menuViewModel = menuViewModel) }
        composable("bluetooth") { ParametreBluetooothScreen(navController = navController, viewModel = bluetoothViewModel, authVm = authViewModel,menuViewModel = menuViewModel) }
        composable("dashboard") { DashboardScreen(navController = navController, viewModel = dashboardViewModel) }
        composable(
            route = "stock?showRevenue={showRevenue}",
            arguments = listOf(
                navArgument("showRevenue") {
                    type = NavType.BoolType
                    defaultValue = true
                }
            )
        ) { backStackEntry ->
            val showRevenue = backStackEntry.arguments?.getBoolean("showRevenue") ?: true
            StockScreen(navController = navController, viewModel = menuViewModel, showRevenue = showRevenue)
        }
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
        composable("voiture") { VoitureHomeScreen( viewModel = menuViewModel, navController = navController) }
        composable("voiture_detail") { VoitureDetailScreen( menuViewModel = menuViewModel, navController = navController) }
        composable("cloture") {
            ClotureScreen(
                viewModel = menuViewModel,
                onBack = { navController.popBackStack() })
        }
        composable("log") {
            LogScreen(
                viewModel = menuViewModel,
                onBack = { navController.popBackStack() })
        }


    }

}


