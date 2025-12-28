package com.example.gestion.composable

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gestion.data.AppTable
import com.example.gestion.data.MenuViewModel
import com.example.gestion.model.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun TableScreen (navController: NavController, menuViewModel: MenuViewModel,authViewModel: AuthViewModel) {

    // Détecte l’orientation et la largeur pour fixer dynamiquement le nombre de colonnes
    val config = LocalConfiguration.current
    val ctx = navController.context
    val screenWidthDp = config.screenWidthDp
    var selectedTab by remember { mutableIntStateOf(0) }
    val tables by menuViewModel.tables.collectAsState()
    val filteredTables = tables.filter { it.active }
    var showDialog by remember { mutableStateOf(false) }
    var tableName by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var tableToDelete by remember { mutableStateOf<AppTable?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val refreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            authViewModel.enqueueSync(
                context = ctx,
                tag = "sync"
            )
            isRefreshing = true
        }
    )
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {

            isRefreshing = false
        }
    }

    val columns = when {
        screenWidthDp >= 900 -> 4
        screenWidthDp >= 600 -> 3
        else -> 2
    }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(refreshState)
        ) {
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                Text(
                    text = "Tables",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (showDialog) {
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
                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Supprimer la table ?") },
                        text = { Text("La table sera désactivée et synchronisée sur vos appareils.") },
                        confirmButton = {
                            TextButton(onClick = {
                                tableToDelete?.let { t ->
                                    menuViewModel.deleteTable(t.id) // active=false, isDirty=true, updatedAt=now()
                                }
                                showDeleteDialog = false
                                tableToDelete = null
                            }) { Text("Supprimer") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showDeleteDialog = false
                                tableToDelete = null
                            }) { Text("Annuler") }
                        }
                    )
                }

                Log.d("Tables", "taille  filtre : ${filteredTables.size}")
                Log.d("Tables", "taille  : ${tables.size}")
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)

                ) {
                    items(filteredTables) { table ->
                        Log.d("Tables", "taille actiu  : ${table.active}")
                        TableListItem(
                            table = table,
                            onClick = {
                                navController.navigate("table_details/${table.id}")
                            },
                            onLongPress = {
                                tableToDelete = table
                                showDeleteDialog = true
                            }
                        )
                    }
                }

            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = refreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TableListItem( table : AppTable, onClick : () -> Unit, onLongPress : () -> Unit) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),

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