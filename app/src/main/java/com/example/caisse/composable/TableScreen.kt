package com.example.caisse.composable

import android.widget.Button
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.caisse.data.MenuViewModel
import androidx.compose.material3.*
import com.example.caisse.data.AppTable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableScreen (navController: NavController, menuViewModel: MenuViewModel) {

    // Détecte l’orientation et la largeur pour fixer dynamiquement le nombre de colonnes
    val config = LocalConfiguration.current
    val orientation = config.orientation
    val screenWidthDp = config.screenWidthDp
    var selectedTab by remember { mutableIntStateOf(0) }
    val tables by menuViewModel.tables.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var tableName by remember { mutableStateOf("") }
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
        },
                floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Table")
            }
        }

    ) {padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(text = "Tables", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp))

            if(showDialog){
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Ajouter une table") },
                    text = {
                        TextField(
                            value = tableName,
                            onValueChange = { tableName = it },
                            label = { Text("Nom de la table") }
                        )
                    },
                    confirmButton = {
                      Button(
                          onClick = {
                              if (tableName.isNotBlank()) {
                                  menuViewModel.addTable(tableName)
                                  showDialog = false
                                  tableName = ""

                              }

                          }
                      ) {
                          Text("Ajouter")
                      }
                    },
                    dismissButton = {
                        Button(onClick = { showDialog = false }) {
                            Text("Annuler")
                        }
                    }
                )
            }
            LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(4),
                modifier = Modifier
                    .weight(1f),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)

            ){
                items(tables){table->
                    TableListItem(
                      table = table,
                      onClick = {
                      navController.navigate("table_details/${table.id}")
                      }
                    )
                }
            }
        }

    }
}


@Composable
fun TableListItem( table : AppTable, onClick : () -> Unit) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        )
        {
            Text(text = table.name, style = MaterialTheme.typography.bodyLarge)
        }


    }

}