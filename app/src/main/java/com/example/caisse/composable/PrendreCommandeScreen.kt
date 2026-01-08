package com.example.caisse.composable

import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.caisse.bluetooth.BluetoothViewModel
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.Produit
import com.example.caisse.data.Ticket
import com.example.caisse.util.drawableUri
import com.example.caisse.util.formatPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrendreCommandeScreen(
    navController: NavController,
    menuViewModel: MenuViewModel,
    bluetoothViewModel: BluetoothViewModel,
) {
    val categories by menuViewModel.categories.collectAsState()
    val products by menuViewModel.produits.collectAsState()
    val cart by menuViewModel.cart.collectAsState()
    val totalPrice by menuViewModel.totalPrice.collectAsState()
    val shopInfos = menuViewModel.getInfos()


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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
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
                        ProductItem(product = product,devise = shopInfos?.devise ?: "") {
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
                    Text("Total    ", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text( formatPrice(totalPrice,shopInfos?.devise), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row (modifier = Modifier
                    .padding(bottom = 8.dp)
                    .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Button(
                        onClick = { if (cart.isNotEmpty()) navController.navigate("panier") },
                        modifier = Modifier.weight(.5f),
                        enabled = cart.isNotEmpty()
                    ) {
                        Text("Valider la commande")
                    }

                    Button(
                        modifier = Modifier.weight(.5f),
                        onClick = {

                            if (!bluetoothViewModel.isConnected.value) {
                                Toast.makeText(
                                    navController.context,
                                    "Pas de device connecté",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }else{
                                bluetoothViewModel.printInvoice(cart, totalPrice,shopInfos)
                                Toast.makeText(
                                    navController.context,
                                    "Ticket imprimé",
                                    Toast.LENGTH_SHORT
                                ).show()
                                navController.popBackStack() // revenir en arrière après validation
                            }
                        },
                        enabled = totalPrice > 0
                    ) {
                        Text("Imprimer",maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductItem(product: Produit, devise : String, onProductClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp) // zone cliquable bien grande
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isPressed = true
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        isPressed = false
                        onProductClick()
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        isPressed = false
                        true
                    }
                    else -> false
                }
            },
        elevation = cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPressed)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            else
                Color.White
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = product.image ?: drawableUri("placeholder_image"),
                    contentDescription = product.nom,
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color.Transparent),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                product.nom,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 2, // Limite à 2 lignes pour éviter que ça déborde
                lineHeight = 14.sp
            )
            Text(formatPrice(product.prix,devise), fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary, // Couleur pour distinguer le prix
                fontWeight = FontWeight.Bold)
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
@Composable
fun ProductItemHorizontal(product: Produit, devise: String,onProductClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp) // Hauteur fixe et compacte
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isPressed = true
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        isPressed = false
                        onProductClick()
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        isPressed = false
                        true
                    }
                    else -> false
                }
            },
        elevation = cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = if (isPressed) Color.LightGray else Color.White),
        shape = MaterialTheme.shapes.medium // Coins moins arrondis pour gagner de la place
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image à gauche
            AsyncImage(
                model = product.image ?: drawableUri("placeholder_image"),
                contentDescription = product.nom,
                modifier = Modifier
                    .size(48.dp)
                    .padding(4.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Textes à droite
            Column(
                modifier = Modifier.weight(1f), // Prend tout l'espace restant
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = product.nom,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text =  formatPrice(product.prix,devise),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}