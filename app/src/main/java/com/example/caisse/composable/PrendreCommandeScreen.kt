package com.example.caisse.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.caisse.R
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.Produit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrendreCommandeScreen(
    navController: NavController,
    menuViewModel: MenuViewModel = viewModel()
) {
    val categories by menuViewModel.categories.collectAsState()
    val products by menuViewModel.products.collectAsState()
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.id?: "") }
    val cart = remember { mutableStateListOf<Produit>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Take Order") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("$${cart.sumOf { it.prix }}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { /* TODO: Handle order validation */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Validate Order")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            val selectedIndex = categories.indexOfFirst { it.id == selectedCategory }
            TabRow(selectedTabIndex = selectedIndex.coerceAtLeast(0)) {
                categories.forEach { category ->
                    Tab(
                        selected = category.id == selectedCategory,
                        onClick = { selectedCategory = category.id },
                        text = { Text(category.name) }
                    )
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(products.filter { it.categoryId  == selectedCategory }) { product ->
                    ProductItem(product = product) {
                        if (cart.contains(product)) {
                            cart.remove(product)
                        } else {
                            cart.add(product)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductItem(product: Produit, onProductClick: () -> Unit) {
    Card(
        modifier = Modifier.clickable(onClick = onProductClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.placeholder_image),
                contentDescription = product.nom,
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(product.nom, fontWeight = FontWeight.Bold)
            Text("$${product.prix}")
        }
    }
}