package com.example.epicerie.composable

import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.epicerie.bluetooth.BluetoothViewModel
import com.example.epicerie.data.MenuViewModel
import com.example.epicerie.data.Produit
import com.example.epicerie.data.Ticket
import com.example.epicerie.util.FavorisPrefs
import com.example.epicerie.util.drawableUri
import com.example.epicerie.util.formatPrice
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var scanBuffer by remember { mutableStateOf("") } // Stocke les chiffres scannés
    var scanJob by remember { mutableStateOf<Job?>(null) }

    // Force le focus sur l'écran dès qu'il s'ouvre pour capter le scan
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id) }

    // Update selected category if the initial one is removed or not available
    LaunchedEffect(categories) {
        if (selectedCategoryId == null || categories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = categories.firstOrNull()?.id
        }
    }

    // Onglet "Favoris" : affiché avant les catégories, produits ajoutés par appui long sur
    // leur carte (voir ProductItem). Stockage local au pad (FavorisPrefs), pas de synchro.
    val favContext = LocalContext.current
    var showFavoris by remember { mutableStateOf(false) }
    var favoriIds by remember { mutableStateOf(FavorisPrefs.getFavoris(favContext)) }
    val toggleFavori: (Produit) -> Unit = { product ->
        val nowFavori = FavorisPrefs.toggleFavori(favContext, product.id)
        favoriIds = FavorisPrefs.getFavoris(favContext)
        Toast.makeText(
            favContext,
            if (nowFavori) "${product.nom} ajouté aux favoris" else "${product.nom} retiré des favoris",
            Toast.LENGTH_SHORT
        ).show()
    }
    val displayedProducts = remember(products, showFavoris, selectedCategoryId, favoriIds) {
        if (showFavoris) products.filter { favoriIds.contains(it.id.toString()) }
        else products.filter { it.categoryId == selectedCategoryId }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    val char = event.nativeKeyEvent.unicodeChar.toChar()

                    // On accepte les chiffres et les lettres (au cas où le code barre contient des lettres)
                    if (char.isLetterOrDigit()) {

                        // 3. À chaque touche, on annule le précédent timer
                        scanJob?.cancel()

                        // On ajoute le caractère au buffer
                        scanBuffer += char

                        // 4. On lance un nouveau timer
                        scanJob = scope.launch {
                            // On attend un court instant (ex: 400ms) pour voir si d'autres chiffres arrivent
                            delay(400)

                            // --- FIN DU SCAN DÉTECTÉE (SILENCE) ---
                            if (scanBuffer.isNotEmpty()) {
                                val codeScanned = scanBuffer

                                // Recherche du produit
                                val productFound = products.find { it.codeBarre == codeScanned }

                                if (productFound != null) {
                                    menuViewModel.addToCart(productFound)
                                    Toast.makeText(navController.context, "Ajouté : ${productFound.nom}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(navController.context, "Inconnu : $codeScanned", Toast.LENGTH_SHORT).show()
                                }

                                // Reset du buffer pour le prochain scan
                                scanBuffer = ""
                            }
                        }
                        true // On a traité l'événement
                    } else {
                        false
                    }
                } else {
                    false
                }
            },
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
                    val selectedIndex = if (showFavoris) 0
                        else categories.indexOfFirst { it.id == selectedCategoryId }.coerceAtLeast(0) + 1
                    ScrollableTabRow(selectedTabIndex = selectedIndex) {
                        Tab(
                            selected = showFavoris,
                            onClick = { showFavoris = true },
                            text = { Text("★ Favoris") }
                        )
                        categories.forEach { category ->
                            Tab(
                                selected = !showFavoris && category.id == selectedCategoryId,
                                onClick = { showFavoris = false; selectedCategoryId = category.id },
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
                    if (showFavoris && displayedProducts.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                "Aucun favori pour l'instant — appui long sur un produit pour l'ajouter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            )
                        }
                    }
                    items(displayedProducts) { product ->
                        ProductItem(
                            product = product,
                            devise = shopInfos?.devise ?: "",
                            isFavori = favoriIds.contains(product.id.toString()),
                            onProductClick = { menuViewModel.addToCart(product) },
                            onToggleFavori = { toggleFavori(product) }
                        )
                    }
                }
            }

            // Cart Area
            var showClearCartDialog by remember { mutableStateOf(false) }
            if (showClearCartDialog) {
                AlertDialog(
                    onDismissRequest = { showClearCartDialog = false },
                    title = { Text("Vider le panier ?") },
                    text = { Text("Tous les articles du panier seront retirés. Cette action est irréversible.") },
                    confirmButton = {
                        TextButton(onClick = {
                            menuViewModel.clearCart()
                            showClearCartDialog = false
                        }) { Text("Vider") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearCartDialog = false }) { Text("Annuler") }
                    }
                )
            }
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Panier", style = MaterialTheme.typography.headlineSmall)
                    IconButton(
                        onClick = { showClearCartDialog = true },
                        enabled = cart.isNotEmpty()
                    ) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = "Vider le panier")
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(cart, key = { it.produit.id }) { ticket ->
                        CartItemRow(
                            ticket = ticket,
                            onIncrease = { menuViewModel.increaseQuantity(ticket.produit.id) },
                            onDecrease = { menuViewModel.decreaseQuantity(ticket.produit.id) },
                            devise = shopInfos?.devise ?: ""
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
                    // Le ticket est désormais imprimé après validation de la vente (écran
                    // Panier), pas avant : imprimer ici permettait d'imprimer un ticket pour
                    // une vente jamais enregistrée en base.
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
}

@Composable
fun ProductItem(
    product: Produit,
    devise: String,
    isFavori: Boolean = false,
    onProductClick: () -> Unit,
    onToggleFavori: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp) // zone cliquable bien grande
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onProductClick,
                onLongClick = onToggleFavori
            ),
        elevation = cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = MaterialTheme.shapes.large,
    ) {
        Box(Modifier.fillMaxSize()) {
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

            if (onToggleFavori != null) {
                Icon(
                    imageVector = if (isFavori) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isFavori) "Retirer des favoris (appui long)" else "Ajouter aux favoris (appui long)",
                    tint = if (isFavori) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(16.dp)
                )
            }
        }
    }
}
@Composable
fun CartItemRow(
    ticket: Ticket,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    devise: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.weight(1f)
        )
        {
            Text(
                text = "${ticket.produit.nom}  "
            )
            Text(
                text = formatPrice(ticket.produit.prix, devise)
            )
            Text(
                text = " X ${ticket.quantity}   "
            )

            Text(
                text = formatPrice(ticket.produit.prix * ticket.quantity, devise)
            )
        }
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