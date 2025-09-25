package com.example.caisse.composable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.caisse.data.MenuViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanierScreen(
    navController: NavController,
    menuViewModel: MenuViewModel
) {
    val cart by menuViewModel.cart.collectAsState()
    val totalPrice by menuViewModel.totalPrice.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Récapitulatif du Panier") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(cart) { ticket ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${ticket.produit.nom} x${ticket.quantity}")
                        Text(String.format("%.2f €", ticket.produit.prix * ticket.quantity))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(String.format("%.2f €", totalPrice), fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { /* TODO: Finalize order logic */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirmer la commande")
            }
        }
    }
}