package com.example.caisse.composable

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.caisse.bluetooth.BluetoothViewModel
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.Ticket
import com.example.caisse.model.AuthViewModel
import com.example.caisse.util.formatPrice
import com.google.firebase.auth.auth
import java.util.UUID
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.rememberModalBottomSheetState

@RequiresApi(Build.VERSION_CODES.O)
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

    LaunchedEffect(categories) {
        if (selecredCategoryID == null || categories.none { it.id == selecredCategoryID }) {
            selecredCategoryID = categories.firstOrNull()?.id
        }
    }
    val tableItems by menuViewModel.tableItems.collectAsState()

    val ctx = navController.context

    // Load items when the screen is displayed for the first time
    LaunchedEffect(tableUuid) {
        menuViewModel.loadTableItems(tableUuid)
    }

    // Clear items when the user leaves the screen
    DisposableEffect(tableUuid) {
        val uid = com.google.firebase.Firebase.auth.currentUser?.uid
        var reg: com.google.firebase.firestore.ListenerRegistration? = null
        if (uid != null) {
            val cloud = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            reg = cloud.collection("users").document(uid)
                .collection("table_items")
                .whereEqualTo("tableId", tableUuid.toString())
                .addSnapshotListener { snap, _ ->
                    if (snap != null) {
                        // Recharger depuis Room (pull a déjà upsert) ou reconstruire localement
                        // Ici on recharge proprement via Room -> VM
                        menuViewModel.loadTableItems(tableUuid)
                    }
                }
        }
        onDispose { reg?.remove() }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Table : ${table?.name}") },
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

        // ✅ Total déjà dispo (pour l’afficher partout)
        val totaltable by menuViewModel.totalAmount.collectAsState()

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val isCompact = maxWidth < 600.dp
            var showBill by remember { mutableStateOf(false) }

            // =======================
            // COMPACT: Produits plein écran + Facture en BottomSheet
            // =======================
            if (isCompact) {

                // ---- BottomSheet Facture
                if (showBill) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ModalBottomSheet(
                        onDismissRequest = { showBill = false },
                        sheetState = sheetState
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {

                            // ✅ Total en haut du sheet
                            Row(
                                modifier = Modifier.fillMaxWidth().weight(0.1f),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Facture", style = MaterialTheme.typography.headlineSmall)
                                Text(
                                    text = formatPrice(totaltable, info?.devise),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            LazyColumn (modifier = Modifier.weight(0.8f)) {
                                itemsIndexed(
                                    tableItems,
                                    key = { index, ticket -> "${ticket.produit.id}@$index" }
                                ) { _, ticket ->
                                    TableItemRow(
                                        ticket = ticket,
                                        onIncrease = {
                                            menuViewModel.updateTableItemQuantity(
                                                ticket.produit.id, tableUuid, ticket.quantity + 1
                                            )
                                        },
                                        onDecrease = {
                                            menuViewModel.updateTableItemQuantity(
                                                ticket.produit.id, tableUuid, ticket.quantity - 1
                                            )
                                        }
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth().weight(0.1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        menuViewModel.payTable(tableUuid)
                                        navController.popBackStack()
                                        authVm.enqueueSync(context = ctx, tag = "sync")
                                    },
                                    enabled = totaltable > 0
                                ) { Text("Valider", maxLines = 1) }

                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (!bluetoothViewModel.isConnected.value) {
                                            Toast.makeText(ctx, "Pas de device connecté", Toast.LENGTH_SHORT).show()
                                            menuViewModel.loggerEvenement("Erreur d'impression", "Pas de device connecté")
                                            return@Button
                                        } else {
                                            bluetoothViewModel.printProforma(tableItems, totaltable, info)
                                            Toast.makeText(ctx, "Ticket imprimé", Toast.LENGTH_SHORT).show()
                                            navController.popBackStack()
                                        }
                                    },
                                    enabled = totaltable > 0
                                ) { Text("Imprimer", maxLines = 1) }
                            }

                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }

                // ---- Produits plein écran (colonne gauche seulement)
                Column(modifier = Modifier.fillMaxSize()) {

                    if (categories.isNotEmpty()) {

                        // s’assurer que l’ID sélectionné est valide
                        if (selecredCategoryID == null || categories.none { it.id == selecredCategoryID }) {
                            selecredCategoryID = categories.first().id
                        }

                        val selectedIndex =
                            categories.indexOfFirst { it.id == selecredCategoryID }
                                .coerceAtLeast(0)
                                .coerceAtMost(categories.lastIndex)

                        ScrollableTabRow(
                            selectedTabIndex = selectedIndex,
                            edgePadding = 12.dp
                        ) {
                            categories.forEach { category ->
                                Tab(
                                    selected = category.id == selecredCategoryID,
                                    onClick = { selecredCategoryID = category.id },
                                    text = {
                                        Text(
                                            text = category.name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    } else {
                        // optionnel : un placeholder pendant le chargement
                        Text(
                            text = "Chargement...",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }



                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 110.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(products.filter { it.categoryId == selecredCategoryID }) { product ->
                            ProductItemHorizontal(product = product, devise = info?.devise ?: "") {
                                menuViewModel.addProductToTable(product.id, tableUuid)
                            }
                        }
                    }
                }

                // ---- Bouton flottant "Facture" (badge = nb articles)
                Box(modifier = Modifier.fillMaxSize()) {
                    FloatingActionButton(
                        onClick = { showBill = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                val qty = tableItems.sumOf { it.quantity }
                                if (qty > 0) Badge { Text(qty.toString()) }
                            }
                        ) {
                            Text("Facture")
                        }
                    }
                }
            }

            // =======================
            // LARGE: ton affichage actuel 2 colonnes (quasi inchangé)
            // =======================
            else {
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Gauche: produits
                    Column(
                        modifier = Modifier
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
                                ProductItemHorizontal(product = product, devise = info?.devise ?: "") {
                                    menuViewModel.addProductToTable(product.id, tableUuid)
                                }
                            }
                        }
                    }

                    // Droite: facture (comme avant, avec total en bas)
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.35f)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Facture",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            itemsIndexed(tableItems, key = { index, ticket -> "${ticket.produit.id}@$index" }) { _, ticket ->
                                TableItemRow(
                                    ticket = ticket,
                                    onIncrease = {
                                        menuViewModel.updateTableItemQuantity(
                                            ticket.produit.id, tableUuid, ticket.quantity + 1
                                        )
                                    },
                                    onDecrease = {
                                        menuViewModel.updateTableItemQuantity(
                                            ticket.produit.id, tableUuid, ticket.quantity - 1
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                formatPrice(totaltable, info?.devise),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    menuViewModel.payTable(tableUuid)
                                    navController.popBackStack()
                                    authVm.enqueueSync(context = ctx, tag = "sync")
                                },
                                enabled = totaltable > 0
                            ) { Text("Valider", maxLines = 1) }

                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (!bluetoothViewModel.isConnected.value) {
                                        Toast.makeText(ctx, "Pas de device connecté", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    } else {
                                        bluetoothViewModel.printProforma(tableItems, totaltable, info)
                                        Toast.makeText(ctx, "Ticket imprimé", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    }
                                },
                                enabled = totaltable > 0
                            ) { Text("Imprimer", maxLines = 1) }
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