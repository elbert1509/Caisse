package com.example.caisse.composable

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.caisse.bluetooth.BluetoothViewModel
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.Ticket
import com.example.caisse.data.UserRole
import com.example.caisse.model.AuthViewModel
import com.example.caisse.util.formatPrice
import com.google.firebase.auth.auth
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableDetailsScreen (navController: NavController, menuViewModel: MenuViewModel, tableId: String, bluetoothViewModel: BluetoothViewModel,authVm: AuthViewModel){
    var selectedTab by remember { mutableIntStateOf(0) }
    val tables by menuViewModel.tables.collectAsState()
    val tableUuid = remember(tableId) { UUID.fromString(tableId) }
    val table = tables.find { it.id == tableUuid }
    val categories by menuViewModel.categories.collectAsState()
    val products by menuViewModel.produits.collectAsState()
    val info = menuViewModel.getInfos()
    var selecredCategoryID by remember { mutableStateOf(categories.firstOrNull()?.id) }
    val session by menuViewModel.sessionProfile.collectAsState()
    val vendeurName = menuViewModel.getVendeurNameById(table?.vendeurId)
    val role = session?.role

    LaunchedEffect(tableUuid) {
        menuViewModel.setCurrentTable(tableUuid)
    }

    DisposableEffect(Unit) {
        onDispose { menuViewModel.setCurrentTable(null) }
    }
    LaunchedEffect(categories) {
        if (selecredCategoryID == null || categories.none { it.id == selecredCategoryID }) {
            selecredCategoryID = categories.firstOrNull()?.id
        }
    }
    val tableItems by menuViewModel.tableItems.collectAsState()

    val ctx = navController.context

    LaunchedEffect(tableUuid, session.role, session.vendeurId) {
        menuViewModel.loadTableItems(tableUuid)
    }




    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Table : ${table?.name}   Serveur : $vendeurName") },
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
                onTabSelected = { },
                navController = navController
            )
        }
    ) { padding ->

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(    modifier = Modifier
                .fillMaxHeight()
                .weight(0.65f)
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp)
            ) {

                TabRow(
                    selectedTabIndex = categories.indexOfFirst { it.id == selecredCategoryID }
                        .coerceAtLeast(0)
                ) {
                    categories.forEach { category ->
                        Tab(
                            selected = category.id == selecredCategoryID,
                            onClick = { selecredCategoryID = category.id },
                            text = { Text(category.name) }
                        )
                    }

                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 128.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(products.filter { it.categoryId == selecredCategoryID }) { product ->
                        ProductItemHorizontal(product = product,devise = info?.devise ?: "") {
                            menuViewModel.addProductToTable(product.id, tableUuid)
                        }
                    }
                }

            }
            Column(modifier = Modifier
                .fillMaxHeight()
                .weight(0.35f)
                .padding(16.dp)
            )
            {
                Text(
                    text = "Facture",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                //Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    itemsIndexed(tableItems, key ={ index, ticket -> "${ticket.produit.id}@$index" }) {  _,ticket ->
                        TableItemRow(
                            ticket = ticket,
                            onIncrease = {
                                menuViewModel.updateTableItemQuantity(
                                ticket.produit.id,
                                tableUuid,
                                ticket.quantity + 1
                                ) },
                            onDecrease = {
                                menuViewModel.updateTableItemQuantity(
                                    ticket.produit.id,
                                    tableUuid,
                                    ticket.quantity - 1

                                )
                            }
                        )

                    }
                }


                val totaltable  by menuViewModel.totalAmount.collectAsState()
                Spacer(Modifier.height(8.dp))

                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween

                ){
                    Text(text = "Total", style = MaterialTheme.typography.headlineSmall)
                    Text(text = formatPrice(totaltable,info?.devise),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(16.dp))

                if (role == UserRole.GERANT){
                    Row (modifier = Modifier
                        .padding(bottom = 8.dp)
                        .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                menuViewModel.payTable(tableUuid)
                                navController.popBackStack() // revenir en arrière après validation
                                authVm.enqueueSync(
                                    context = ctx,
                                    tag = "sync"
                                )
                            },
                            enabled = totaltable > 0
                        ) {
                            Text("Valider", maxLines = 1)
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {

                                if (!bluetoothViewModel.isConnected.value) {
                                    Toast.makeText(
                                        navController.context,
                                        "Pas de device connecté",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }else{
                                    bluetoothViewModel.printInvoice(tableItems, totaltable,info)
                                    Toast.makeText(
                                        navController.context,
                                        "Ticket imprimé",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.popBackStack() // revenir en arrière après validation
                                }


                            },
                            enabled = totaltable > 0
                        ) {
                            Text("Imprimer",maxLines = 1)
                        }
                    }
                }


            }


        }



    }

}

@Composable
fun TableItemRow(
    ticket : Ticket,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
)
{
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${ticket.produit.nom} (x${ticket.quantity})",
            modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrease, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Remove, "Diminuer")
            }
            IconButton(onClick = onIncrease, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Add, "Augmenter")
            }
        }
    }
}