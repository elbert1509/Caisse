package com.example.caisse.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.caisse.data.Category
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.Produit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    viewModel: MenuViewModel
) {
    val products by viewModel.produits.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var newPrice by remember { mutableStateOf(TextFieldValue("")) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    var renameTarget by remember { mutableStateOf<Produit?>(null) }
    var renameText by remember { mutableStateOf(TextFieldValue("")) }
    var renamePrice by remember { mutableStateOf(TextFieldValue("")) }
    var renameCategory by remember { mutableStateOf<Category?>(null) }

    var productText by remember { mutableStateOf(TextFieldValue("")) }

    // Liste filtrée en fonction de la recherche
    val filteredProducts = remember(products, productText.text, selectedCategory) {
        products.filter { produit ->
            val matchesText =
                productText.text.isBlank() ||
                        produit.nom.contains(productText.text, ignoreCase = true)

            val matchesCategory =
                selectedCategory == null || produit.categoryId == selectedCategory!!.id

            matchesText && matchesCategory
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Produits", style = MaterialTheme.typography.headlineSmall) }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Add Product Form
            OutlinedTextField(
                value = productText,
                onValueChange = { productText = it },
                label = { Text("Nom du produit (ajouter / rechercher)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = newPrice,
                onValueChange = { newPrice = it },
                label = { Text("Prix") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            CategoryDropdown(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val name = productText .text.trim()
                    val price = newPrice.text.trim().toDoubleOrNull()
                    val category = selectedCategory
                    if (name.isNotEmpty() && price != null && category != null) {
                        viewModel.addProduit(name, price, category.id)
                        productText  = TextFieldValue("")
                        newPrice = TextFieldValue("")
                        selectedCategory = null
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Ajouter")
            }

            Spacer(Modifier.height(16.dp))

            // Product List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    ProductRow(
                        product = product,
                        categoryName = categories.find { it.id == product.categoryId }?.name ?: "Inconnue",
                        onRename = {
                            renameTarget = product
                            renameText = TextFieldValue(product.nom)
                            renamePrice = TextFieldValue(product.prix.toString())
                            renameCategory = categories.find { it.id == product.categoryId }
                        },
                        onDelete = { viewModel.deleteProduit(product) }
                    )
                }
            }
        }

        // Rename/Edit Dialog
        if (renameTarget != null) {
            AlertDialog(
                onDismissRequest = { renameTarget = null },
                title = { Text("Modifier le produit") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = renameText,
                            onValueChange = { renameText = it },
                            label = { Text("Nom") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = renamePrice,
                            onValueChange = { renamePrice = it },
                            label = { Text("Prix") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        CategoryDropdown(
                            categories = categories,
                            selectedCategory = renameCategory,
                            onCategorySelected = { renameCategory = it }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val name = renameText.text.trim()
                        val price = renamePrice.text.trim().toDoubleOrNull()
                        val category = renameCategory
                        val target = renameTarget
                        if (target != null && name.isNotEmpty() && price != null && category != null) {
                            val updatedProduct = target.copy(
                                nom = name,
                                prix = price,
                                categoryId = category.id
                            )
                            viewModel.updateProduit(updatedProduct)
                            renameTarget = null
                        }
                    }) { Text("Enregistrer") }
                },
                dismissButton = {
                    TextButton(onClick = { renameTarget = null }) { Text("Annuler") }
                }
            )
        }
    }
}

@Composable
fun CategoryDropdown(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = selectedCategory?.name ?: "Sélectionner une catégorie",
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}


@Composable
private fun ProductRow(
    product: Produit,
    categoryName: String,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.nom, style = MaterialTheme.typography.bodyLarge)
                Text("${product.prix} € - ($categoryName)", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onRename) {
                Icon(Icons.Default.Edit, contentDescription = "Modifier")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Supprimer")
            }
        }
    }
}