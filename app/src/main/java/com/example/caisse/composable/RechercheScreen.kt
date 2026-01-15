package com.example.piece.composable

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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.piece.data.MenuViewModel
import com.example.piece.data.Produit
import com.example.piece.util.formatPrice

@Composable
fun RechercheScreen(
    viewModel: MenuViewModel
) {
    var query by remember { mutableStateOf("") }
    val produits by viewModel.produits.collectAsState()
    val devise = viewModel.getInfos()?.devise ?: "FCFA"
// Ajout pour mettre le focus automatiquement sur le champ de recherche
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val filteredProduits = if (query.isEmpty()) {
        produits
    } else {
        produits.filter { produit ->
            val matchNom = produit.nom.contains(query, ignoreCase = true)
            // On vérifie si le code barre existe et s'il contient la recherche
            val matchCode = produit.codeBarre?.contains(query, ignoreCase = true) == true

            matchNom || matchCode
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // 🔍 Champ de recherche
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Nom ou scan code-barres") }, // Label mis à jour
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester), // Focus automatique
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Recherche")
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Effacer")
                    }
                }
            },
            singleLine = true // Important pour éviter les retours à la ligne du scanner
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 🧾 Résultats
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            items(filteredProduits, key = { it.id }) { produit ->
                ProduitSearchItem(
                    produit = produit,
                    devise = devise,
                )
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
