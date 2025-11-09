package com.example.caisse.composable

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DataExploration
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Liquor
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.ShopInfos

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionScreen(navController: NavController, viewModel: MenuViewModel){
    val context = LocalContext.current
    var unlocked by remember { mutableStateOf(false) }
    val infos = viewModel.getInfos()


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Gestion",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Carte de garde par mot de passe / ou directement les boutons si déverrouillé
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = cardElevation(6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Spacer(Modifier.height(12.dp))

                    if (!unlocked) {
                        PasswordGate(
                            onUnlock = { unlocked = true },
                            context = context,
                            infos = infos
                        )
                    } else {
                        Text(
                            "Accès autorisé",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Choisissez une section à gérer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (unlocked) {
                // Grille d’actions (cohérente avec Home)
                val config = LocalConfiguration.current
                val screenWidthDp = config.screenWidthDp
                val columns = when {
                    screenWidthDp >= 900 -> 4
                    else -> 2
                }
                val tiles = remember {
                    listOf(
                        GestionTile("Inventaire", Icons.Default.Liquor) { navController.navigate("inventaire") },
                        GestionTile("Produits", Icons.Default.Inventory) { navController.navigate("produit") },
                        GestionTile("Catégories", Icons.Default.Category) { navController.navigate("categorie") },
                        GestionTile("Donnée", Icons.Default.DataExploration) { navController.navigate("donnee") },
                        GestionTile("Stock", Icons.Default.Warehouse) { navController.navigate("stock") },
                        GestionTile("Dashboard", Icons.Default.Warehouse) { navController.navigate("Dashboard") },
                        GestionTile("Vente", Icons.Default.PointOfSale) { navController.navigate("vente") },

                    )
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = cardElevation(6.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Sections",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(tiles) { tile ->
                                GestionActionButton(tile)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class GestionTile(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun GestionActionButton(tile: GestionTile) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { tile.onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = cardElevation(6.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(tile.icon, contentDescription = tile.title, tint = Color.White)
            Spacer(Modifier.height(6.dp))
            Text(tile.title, color = Color.White)
        }
    }
}

/* ---------- Password Gate ---------- */

@Composable
private fun PasswordGate(
    onUnlock: () -> Unit,
    context: Context,
    infos: ShopInfos?
) {
    var pwd by remember { mutableStateOf("") }
    var pwd2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Mot de passe requis",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Veuillez saisir votre mot de passe pour accéder.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = pwd,
            onValueChange = { pwd = it; error = null },
            label = { Text("Mot de passe") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (!error.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                enabled = pwd.isNotBlank(),
                onClick = {
                    val ok = checkPwd( pwd,infos)
                    if (ok) onUnlock() else error = "Mot de passe incorrect."
                }
            ) { Text("Entrer") }
        }
    }
}

/* ---------- Storage (SharedPreferences simple) ---------- */


private fun checkPwd( pwd: String, infos: ShopInfos?): Boolean {
    val saved = infos?.password
    return saved != null && saved == pwd
}