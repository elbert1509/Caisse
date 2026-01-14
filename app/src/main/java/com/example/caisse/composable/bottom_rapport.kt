package com.example.piece.composable

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview

/**
 * Barre de navigation inférieure avec 3 icônes : Home, Prendre commande, Historique
 * À intégrer dans un Scaffold :
 *
 * Scaffold(
 *   bottomBar = { BottomHome(onTabSelected = { index -> /* handle */ }) }
 * )
 */

@Composable
fun Bottom_rapport(
    selectedIndex: Int = 0,
    onExportClick: (tabIndex: Int) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedIndex == 0,
            onClick = { onExportClick(0)  },
            icon = { Icon(Icons.Filled.Share, contentDescription = "Rapport Journalier") },
            label = { Text("Rapport Journalier ") }
        )
        NavigationBarItem(
            selected = selectedIndex == 1,
            onClick = { onExportClick(1) },
            icon = { Icon(Icons.Filled.Share, contentDescription = "Rapport Hebdo") },
            label = { Text("Rapport Hebdo") }
        )
        NavigationBarItem(
            selected = selectedIndex == 2,
            onClick = { onExportClick(2) },
            icon = { Icon(Icons.Filled.Share, contentDescription = "Rapport Mensuel") },
            label = { Text("Rapport Mensuel") }
        )
    }
}


@Composable
@Preview
fun BottomRapportPreview() {
    var selectedIndex by remember { mutableStateOf(0) }
    BottomHome(
        onTabSelected = { index -> selectedIndex = index },
        selectedIndex = selectedIndex
    )
}