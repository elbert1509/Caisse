package com.example.caisse.composable

import android.os.Build
import android.view.MotionEvent
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.caisse.bluetooth.BluetoothViewModel
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.Produit
import com.example.caisse.data.Ticket
import com.example.caisse.util.drawableUri
import com.example.caisse.util.formatPrice
import androidx.compose.foundation.layout.BoxWithConstraints

@RequiresApi(Build.VERSION_CODES.O)
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

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val isCompact = maxWidth < 600.dp
            var showCart by remember { mutableStateOf(false) }

            // ══ COMPACT (téléphone) : produits plein écran + barre totale fixe ══
            if (isCompact) {

                // Panier en BottomSheet
                if (showCart) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ModalBottomSheet(
                        onDismissRequest = { showCart = false },
                        sheetState = sheetState
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Panier", style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(8.dp))

                            LazyColumn(modifier = Modifier.weight(0.8f)) {
                                items(cart, key = { it.produit.id }) { ticket ->
                                    CartItemRow(
                                        ticket = ticket,
                                        devise = shopInfos?.devise ?: "",
                                        onIncrease = { menuViewModel.increaseQuantity(ticket.produit.id) },
                                        onDecrease = { menuViewModel.decreaseQuantity(ticket.produit.id) }
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(0.1f),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Total",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    formatPrice(totalPrice, shopInfos?.devise),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(0.1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { if (cart.isNotEmpty()) navController.navigate("panier") },
                                    modifier = Modifier.weight(1f),
                                    enabled = cart.isNotEmpty()
                                ) { Text("Valider") }

                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (!bluetoothViewModel.isConnected.value) {
                                            Toast.makeText(navController.context, "Pas de device connecté", Toast.LENGTH_SHORT).show()
                                            menuViewModel.loggerEvenement("Impression Annulée", "Impression annulée car pas de device connecté")
                                            return@Button
                                        } else {
                                            bluetoothViewModel.printProforma(cart, totalPrice, shopInfos)
                                            Toast.makeText(navController.context, "Ticket imprimé", Toast.LENGTH_SHORT).show()
                                            navController.popBackStack()
                                        }
                                    },
                                    enabled = totalPrice > 0
                                ) { Text("Imprimer", maxLines = 1) }
                            }

                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }

                // Produits + barre totale persistante
                Column(modifier = Modifier.fillMaxSize()) {

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
                        columns = GridCells.Adaptive(minSize = 110.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(products.filter { it.categoryId == selectedCategoryId }) { product ->
                            ProductItem(product = product, devise = shopInfos?.devise ?: "") {
                                menuViewModel.addToCart(product)
                            }
                        }
                    }

                    // Barre totale persistante — remplace le FAB flottant
                    AnimatedVisibility(
                        visible = cart.isNotEmpty(),
                        enter = slideInVertically { it },
                        exit = slideOutVertically { it }
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 12.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "${cart.sumOf { it.quantity }} article(s)",
                                        color = Color.White.copy(alpha = 0.8f),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        formatPrice(totalPrice, shopInfos?.devise),
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Button(
                                    onClick = { showCart = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text("Voir le panier", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // ══ LARGE (tablette / grand écran) : layout côte à côte ══
            else {
                Row(modifier = Modifier.fillMaxSize()) {

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
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(products.filter { it.categoryId == selectedCategoryId }) { product ->
                                ProductItem(product = product, devise = shopInfos?.devise ?: "") {
                                    menuViewModel.addToCart(product)
                                }
                            }
                        }
                    }

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
                                    devise = shopInfos?.devise ?: "",
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
                            Text(
                                "Total",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                formatPrice(totalPrice, shopInfos?.devise),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { if (cart.isNotEmpty()) navController.navigate("panier") },
                                modifier = Modifier.weight(.5f),
                                enabled = cart.isNotEmpty()
                            ) { Text("Valider") }

                            Button(
                                modifier = Modifier.weight(.5f),
                                onClick = {
                                    if (!bluetoothViewModel.isConnected.value) {
                                        Toast.makeText(navController.context, "Pas de device connecté", Toast.LENGTH_SHORT).show()
                                        menuViewModel.loggerEvenement("Impression Annulée", "Impression annulée car pas de device connecté")
                                        return@Button
                                    } else {
                                        bluetoothViewModel.printProforma(cart, totalPrice, shopInfos)
                                        Toast.makeText(navController.context, "Ticket imprimé", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    }
                                },
                                enabled = totalPrice > 0
                            ) { Text("Imprimer", maxLines = 1) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductItem(product: Produit, devise: String, onProductClick: () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "product_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .scale(animatedScale)
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN  -> { scale = 0.94f; true }
                    MotionEvent.ACTION_UP    -> { scale = 1f; onProductClick(); true }
                    MotionEvent.ACTION_CANCEL -> { scale = 1f; true }
                    else -> false
                }
            },
        elevation = cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            AsyncImage(
                model = product.image ?: drawableUri("placeholder_image"),
                contentDescription = product.nom,
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = product.nom,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = formatPrice(product.prix, devise),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CartItemRow(
    ticket: Ticket,
    devise: String = "",
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ticket.produit.nom,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatPrice(ticket.produit.prix, devise)} × ${ticket.quantity}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrease, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "Diminuer",
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = ticket.quantity.toString(),
                modifier = Modifier.width(24.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(onClick = onIncrease, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Augmenter",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ProductItemHorizontal(product: Produit, devise: String, onProductClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN  -> { isPressed = true; true }
                    MotionEvent.ACTION_UP    -> { isPressed = false; onProductClick(); true }
                    MotionEvent.ACTION_CANCEL -> { isPressed = false; true }
                    else -> false
                }
            },
        elevation = cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPressed) MaterialTheme.colorScheme.surfaceVariant else Color.White
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = product.image ?: drawableUri("placeholder_image"),
                contentDescription = product.nom,
                modifier = Modifier
                    .size(48.dp)
                    .padding(4.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = product.nom,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = formatPrice(product.prix, devise),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
