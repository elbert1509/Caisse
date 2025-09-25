package com.example.caisse.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.caisse.R
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.Produit
import com.example.caisse.data.Ticket

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrendreCommandeScreen(
    navController: NavController,
    menuViewModel: MenuViewModel
) {
    val categories by menuViewModel.categories.collectAsState()
    val products by menuViewModel.produits.collectAsState()
    val cart by menuViewModel.cart.collectAsState()
    val totalPrice by menuViewModel.totalPrice.collectAsState()

    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id) }

    // Update selected category if the initial one is removed or not available
    LaunchedEffect(categories) {
        if (selectedCategoryId == null || categories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = categories.firstOrNull()?.id
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prendre une commande") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Product Selection Area
            Column(modifier = Modifier.weight(0.6f)) {
                if (categories.isNotEmpty()) {
                    val selectedIndex = categories.indexOfFirst { it.id == selectedCategoryId }.coerceAtLeast(0)
                    ScrollableTabRow(selectedTabIndex = selectedIndex) {
                        categories.forEach { category ->
                            Tab(
                                selected = category.id == selectedCategoryId,
                                onClick = { selectedCategoryId = category.id },
                                text = { Text(category.name) }
                            )
                        }
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 128.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(products.filter { it.categoryId == selectedCategoryId }) { product ->
                        ProductItem(product = product) {
                            menuViewModel.addToCart(product)
                        }
                    }
                }
            }

            // Cart Area
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .padding(16.dp)
            ) {
                Text("Panier", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(cart, key = { it.produit.id }) { ticket ->
                        CartItemRow(
                            ticket = ticket,
                            onIncrease = { menuViewModel.increaseQuantity(ticket.produit.id) },
                            onDecrease = { menuViewModel.decreaseQuantity(ticket.produit.id) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(String.format("%.2f €", totalPrice), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { if (cart.isNotEmpty()) navController.navigate("panier") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = cart.isNotEmpty()
                ) {
                    Text("Valider la commande")
                }
            }
        }
    }
}

@Composable
fun ProductItem(product: Produit, onProductClick: () -> Unit) {
    Card(
        modifier = Modifier.clickable(onClick = onProductClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = product.image ?: R.drawable.placeholder_image),
                contentDescription = product.nom,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(product.nom, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(String.format("%.2f €", product.prix), fontSize = 12.sp)
        }
    }
}

@Composable
fun CartItemRow(
    ticket: Ticket,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
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