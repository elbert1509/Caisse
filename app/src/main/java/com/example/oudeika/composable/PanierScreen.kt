package com.example.oudeika.composable

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
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.oudeika.data.MenuViewModel
import com.example.oudeika.model.AuthViewModel
import com.example.oudeika.util.formatPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanierScreen(
    navController: NavController,
    menuViewModel: MenuViewModel,
    authVm: AuthViewModel
) {
    val cart by menuViewModel.cart.collectAsState()
    val totalPrice by menuViewModel.totalPrice.collectAsState()
    val ctx = navController.context

    var paiement : Double by remember { mutableStateOf(0.0) }
    val shopInfos = menuViewModel.getInfos()



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
                        Text(formatPrice( ticket.produit.prix * ticket.quantity, shopInfos?.devise))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = if (paiement == 0.0) "" else formatPrice(paiement, shopInfos?.devise),  // Affiche une chaîne vide quand le paiement est 0.0
                onValueChange = { newValue ->
                    // Essaye de convertir la nouvelle valeur en Double
                    try {
                        paiement = newValue.toDouble()
                    } catch (e: NumberFormatException) {
                        // Si la conversion échoue (par exemple, l'utilisateur entre une lettre), on laisse la valeur actuelle
                        paiement = 0.0
                    }
                },
                label = { Text(" Paiement") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Divider()
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(

                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total : ", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(formatPrice(totalPrice, shopInfos?.devise), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (paiement != 0.0) {
                        val monnaie = paiement - totalPrice
                        Text("Monnaie  ", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(formatPrice(monnaie, shopInfos?.devise), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }


            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    menuViewModel.confirmerVente()
                    navController.navigate("home")
                    authVm.enqueueSync(
                        context = ctx,
                        tag = "sync"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirmer la commande")
            }
        }
    }
}