package com.example.piece.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.piece.data.MenuViewModel
import com.example.piece.data.Produit
import com.example.piece.util.formatPrice
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventaireScreen(navController: NavController, viewModel: MenuViewModel) {

    val categories by viewModel.categories.collectAsState()            // onglets
    val produits by viewModel.produits.collectAsState()                // données
    val scope = rememberCoroutineScope()

    // Onglet sélectionné
    var selectedTab by remember { mutableStateOf(0) }

    // Sauvegarde locale (prix & stock) par produit (clé = id)
    val editCache = remember(produits) {
        produits.associate { p -> p.id to mutableStateOf(Editable(p.prix, p.stock)) }
    }
    LaunchedEffect(categories.size) {
        selectedTab = if (categories.isEmpty()) 0 else selectedTab.coerceIn(0, categories.lastIndex)
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Inventaire", style = MaterialTheme.typography.titleLarge)
                        Text("Modifier prix et stock", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            // Bouton global pour sauvegarder toute la catégorie
            if (categories.isNotEmpty()) {
                val catId = categories[selectedTab].id
                val list = produits.filter { it.categoryId == catId && it.isActive }
                val hasDiff = list.any { p ->
                    val e = editCache[p.id]?.value
                    e != null && (e.price != p.prix || e.stock != p.stock)
                }
                Surface(shadowElevation = 8.dp, tonalElevation = 1.dp) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            enabled = hasDiff,
                            onClick = {
                                scope.launch {
                                    list.forEach { p ->
                                        val e = editCache[p.id]?.value ?: return@forEach
                                        if (e.price != p.prix || e.stock != p.stock) {
                                            viewModel.updateProduit(p.copy(prix = e.price, stock = e.stock))
                                        }
                                    }
                                }
                            }
                        ) { Text("Enregistrer la catégorie") }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
        ) {
            // Onglets Catégories
            if (categories.isEmpty()) {
                AssistiveBanner(
                    title = "Aucune catégorie",
                    subtitle = "Crée une catégorie pour commencer, ou ajoute des produits."
                )
            } else {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 12.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    categories.forEachIndexed { index, c ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(c.name) }
                        )
                    }
                }
            }

            val currentProducts = remember(categories, selectedTab, produits) {
                categories.getOrNull(selectedTab)?.let { cat ->
                    produits.filter { it.categoryId == cat.id && it.isActive }
                        .sortedBy { it.nom.lowercase() }
                } ?: emptyList()
            }

            // Liste produits de l’onglet
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    if (currentProducts.isEmpty()) {
                        AssistiveBanner(
                            title = if (categories.isEmpty()) "Aucune catégorie" else "Aucun produit",
                            subtitle = if (categories.isEmpty())
                                "Crée une catégorie pour commencer, ou ajoute des produits."
                            else
                                "Ajoute des produits à cette catégorie pour les modifier ici."
                        )
                    }
                }
                items(currentProducts, key = { it.id }) { produit ->
                    val state = editCache[produit.id]!!.value
                    ProduitCard(
                        produit = produit,
                        editable = state,
                        onChange = { editCache[produit.id]!!.value = it },
                        onReset = {
                            editCache[produit.id]!!.value = Editable(produit.prix, produit.stock)
                        },
                        onSave = {
                            scope.launch {
                                viewModel.updateProduit(
                                    produit.copy(prix = state.price, stock = state.stock)
                                ) // persiste via VM
                            }
                        },
                        devise = viewModel.getInfos()?.devise ?: ""
                    )
                }

                item { Spacer(Modifier.height(72.dp)) } // pour ne pas masquer par la bottomBar
            }
        }
    }
}

/* ---------------------------- UI composables ---------------------------- */

@Composable
private fun ProduitCard(
    produit: Produit,
    editable: Editable,
    onChange: (Editable) -> Unit,
    devise: String,
    onReset: () -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(produit.nom, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedNumberField(
                    label = "Prix $devise",
                    value = editable.price.toStringSafe(),
                    onValueChange = { v -> onChange(editable.copy(price = v.toDoubleOrNullSafe(editable.price))) },
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
                StepperField(
                    label = "Stock",
                    value = editable.stock,
                    onValueChange = { onChange(editable.copy(stock = it.coerceAtLeast(0))) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    formatPrice(editable.price * editable.stock,devise),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onReset) { Text("Réinitialiser") }
                Button(
                    enabled = (editable.price != produit.prix) || (editable.stock != produit.stock),
                    onClick = onSave
                ) { Text("Enregistrer") }
            }
        }
    }
}

@Composable
private fun AssistiveBanner(title: String, subtitle: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/* ---------------------------- Champs / Utils ---------------------------- */

@Composable
private fun OutlinedNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Done
        ),
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun StepperField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(modifier) {
        Row(
            Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            FilledTonalButton(onClick = { onValueChange((value - 1).coerceAtLeast(0)) }) { Text("-") }
            Text(value.toString(), style = MaterialTheme.typography.titleSmall)
            FilledTonalButton(onClick = { onValueChange(value + 1) }) { Text("+") }
        }
    }
}

private data class Editable(val price: Double, val stock: Int)

private fun String.toDoubleOrNullSafe(fallback: Double): Double =
    this.replace(',', '.').toDoubleOrNull() ?: fallback

private fun Double.toStringSafe(): String {
    // évite "1.0" -> "1" qui perturbe l’édition
    return if (this % 1.0 == 0.0) String.format("%.0f", this) else String.format("%.2f", this)
}
