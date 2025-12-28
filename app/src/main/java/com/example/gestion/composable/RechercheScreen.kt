package com.example.gestion.composable

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gestion.data.MenuViewModel
import com.example.gestion.data.Produit
import com.example.gestion.util.formatPrice

@Composable
fun RechercheScreen(
    viewModel: MenuViewModel
) {
    val query by viewModel.searchQuery.collectAsState()
    val produits by viewModel.filteredProduits.collectAsState()
    val devise = viewModel.getInfos()?.devise ?: "FCFA"


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // 🔍 Champ de recherche
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.updateSearchQuery(it) },
            label = { Text("Rechercher un produit") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 🧾 Résultats
        if (produits.isEmpty() && query.length >= 2) {
            Text(
                text = "Aucun produit trouvé",
                color = Color.Gray,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            LazyColumn {
                items(produits, key = { it.id }) { produit ->
                    ProduitSearchItem(
                        produit = produit,
                        devise = devise,
                        )
                }
            }
        }
    }
}

@Composable
fun ProduitSearchItem(
    produit: Produit,
    devise: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Column {
                Text(
                    text = produit.nom,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Stock : ${produit.stock}",
                    fontSize = 13.sp,
                    color = if (produit.stock > 0)
                        Color(0xFF2E7D32) else Color.Red
                )

                Text(
                    text = formatPrice(produit.prix, devise),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }


        }
    }
}
