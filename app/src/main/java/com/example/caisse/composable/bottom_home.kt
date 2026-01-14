package com.example.piece.composable

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController

/**
 * Barre de navigation inférieure avec 3 icônes : Home, Prendre commande, Historique
 * À intégrer dans un Scaffold :
 *
 * Scaffold(
 *   bottomBar = { BottomHome(onTabSelected = { index -> /* handle */ }) }
 * )
 */

@Composable
fun BottomHome(
    onTabSelected: (Int) -> Unit,
    selectedIndex: Int = 0,
    navController: NavController? = null
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedIndex == 0,
            onClick = { navController?.navigate("home") },
            icon = { Icon(Icons.Filled.Home, contentDescription = "Accueil") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = selectedIndex == 1,
            onClick =
                {
                    onTabSelected(1)
                    navController?.navigate("prendre_commande")
                },
            icon = { Icon(Icons.Filled.PointOfSale, contentDescription = "Commande") },
            label = { Text("Commande") }
        )
        NavigationBarItem(
            selected = selectedIndex == 2,
            onClick = { navController?.navigate("historique") },
            icon = { Icon(Icons.Filled.History, contentDescription = "Historique") },
            label = { Text("Historique") }
        )
    }
}


@Composable
@Preview
fun BottomHomePreview() {
    var selectedIndex by remember { mutableStateOf(0) }
    BottomHome(
        onTabSelected = { index -> selectedIndex = index },
        selectedIndex = selectedIndex
    )
}