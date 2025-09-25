package com.example.caisse.composable

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.caisse.data.CategorieViewmodel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrendreCommandeScreen(navController: NavController, category: CategorieViewmodel )
{
    var selectedTab by remember { mutableIntStateOf(1) }


    val listCategory = category.defaultCategories()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Caisse PoS") },
                actions = {
                    IconButton(onClick = { /*TODO*/ }) {
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
        Text(text = "Hello", modifier = Modifier.padding(padding))
    }
}