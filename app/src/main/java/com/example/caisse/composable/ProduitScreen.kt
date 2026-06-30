package com.example.caisse.composable

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.caisse.data.Category
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.Produit
import com.example.caisse.util.PRODUCT_DRAWABLES
import com.example.caisse.util.drawableUri
import com.example.caisse.util.formatPrice


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScreen(
    modifier: Modifier = Modifier,
    viewModel: MenuViewModel,
    navController: NavController
) {
    val products by viewModel.produits.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var newName by remember { mutableStateOf(TextFieldValue("")) }
    var newPrice by remember { mutableStateOf(TextFieldValue("")) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    var renameTarget by remember { mutableStateOf<Produit?>(null) }
    var renameText by remember { mutableStateOf(TextFieldValue("")) }
    var renamePrice by remember { mutableStateOf(TextFieldValue("")) }
    var renameCategory by remember { mutableStateOf<Category?>(null) }
    var renameBarcode by remember { mutableStateOf(TextFieldValue("")) }

    var productText by remember { mutableStateOf(TextFieldValue("")) }
    var deleteTarget by remember { mutableStateOf<Produit?>(null) }
    val context = LocalContext.current

    // Image choisie (formulaire d'ajout) : peut être une URI galerie OU une URI drawable.
    var addImage by remember { mutableStateOf<String?>(null) }
    var showAddDrawablePicker by remember { mutableStateOf(false) }
    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                // ⚠️ persist permission
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                addImage = uri.toString()
            }
        }

    // Image choisie (boîte de modification).
    var editImage by remember { mutableStateOf<String?>(null) }
    var showEditDrawablePicker by remember { mutableStateOf(false) }
    val editImagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) editImage = uri.toString()
        }

    // Liste filtrée en fonction de la recherche, triée par ordre alphabétique (insensible à la casse)
    val filteredProducts = remember(products, productText.text, selectedCategory) {
        products.filter { produit ->
            val matchesText =
                productText.text.isBlank() ||
                        produit.nom.contains(productText.text, ignoreCase = true)

            val matchesCategory =
                selectedCategory == null || produit.categoryId == selectedCategory!!.id

            matchesText && matchesCategory
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.nom })
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
                label = { Text("ajouter / rechercher") },
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { imagePickerLauncher.launch(arrayOf("image/*")) }) {
                    Text(if (addImage == null) "Choisir une photo" else "Changer la photo")
                }
                OutlinedButton(onClick = { showAddDrawablePicker = true }) {
                    Text("Galerie de l'app")
                }
            }
            addImage?.let { img ->
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = img,
                    contentDescription = "Photo produit",
                    modifier = Modifier.size(90.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val name = productText.text.trim()
                    val price = newPrice.text.trim().toDoubleOrNull()
                    val category = selectedCategory
                    if (name.isNotEmpty() && price != null && category != null) {
                        // Interdire un doublon : même nom (insensible à la casse) dans la même catégorie
                        val duplicate = products.any {
                            it.categoryId == category.id &&
                                    it.nom.trim().equals(name, ignoreCase = true)
                        }
                        if (duplicate) {
                            Toast.makeText(
                                context,
                                "« $name » existe déjà dans la catégorie « ${category.name} »",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            viewModel.addProduit(name, price, category.id, imageUri = addImage)
                            productText = TextFieldValue("")
                            newPrice = TextFieldValue("")
                            addImage = null
                            selectedCategory = null
                        }
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
                            editImage = product.image
                        },
                        onDelete = { deleteTarget = product },
                        devise = viewModel.getInfos()?.devise ?: ""
                    )
                }
            }
        }

        // Confirmation de suppression
        deleteTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                title = { Text("Supprimer le produit ?") },
                text = { Text("Voulez-vous vraiment supprimer « ${target.nom} » ? Cette action sera synchronisée sur vos autres appareils.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteProduit(target)
                        deleteTarget = null
                    }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null }) { Text("Annuler") }
                }
            )
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
                        Spacer(Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { editImagePickerLauncher.launch("image/*") }) {
                                Text(if (editImage == null) "Ajouter/Changer photo" else "Changer photo")
                            }
                            OutlinedButton(onClick = { showEditDrawablePicker = true }) {
                                Text("Galerie de l'app")
                            }
                        }

                        editImage?.let { img ->
                            Spacer(Modifier.height(8.dp))

                            AsyncImage(
                                model = img,
                                contentDescription = "Photo produit",
                                modifier = Modifier.size(90.dp)
                            )
                        }
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
                                categoryId = category.id,
                                image = editImage ?: target.image
                            )
                            viewModel.updateProduit(updatedProduct)
                            viewModel.loggerEvenement("Produit modifié", name + " \nancien prix : " + target.prix + "\t\t\t nouveau prix : $price  " )
                            renameTarget = null
                        }
                    }) { Text("Enregistrer") }
                },
                dismissButton = {
                    TextButton(onClick = { renameTarget = null }) { Text("Annuler") }
                }
            )
        }

        // Sélecteur d'image intégrée (formulaire d'ajout)
        if (showAddDrawablePicker) {
            DrawableGalleryDialog(
                onPick = { name ->
                    addImage = drawableUri(name)
                    showAddDrawablePicker = false
                },
                onDismiss = { showAddDrawablePicker = false }
            )
        }

        // Sélecteur d'image intégrée (boîte de modification)
        if (showEditDrawablePicker) {
            DrawableGalleryDialog(
                onPick = { name ->
                    editImage = drawableUri(name)
                    showEditDrawablePicker = false
                },
                onDismiss = { showEditDrawablePicker = false }
            )
        }
    }
}

@Composable
private fun DrawableGalleryDialog(
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Galerie de l'app") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 80.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PRODUCT_DRAWABLES, key = { it }) { name ->
                    AsyncImage(
                        model = drawableUri(name),
                        contentDescription = name,
                        modifier = Modifier
                            .size(80.dp)
                            .clickable { onPick(name) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
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
    modifier: Modifier = Modifier,
    devise : String
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
                Text(formatPrice( product.prix,devise)+"($categoryName)", style = MaterialTheme.typography.bodySmall)
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