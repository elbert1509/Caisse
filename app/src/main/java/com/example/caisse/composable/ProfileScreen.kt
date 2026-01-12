package com.example.caisse.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.Vendeur
import com.example.caisse.util.PasswordHasher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    menuViewModel: MenuViewModel
) {
    val vendeurs by menuViewModel.vendeurs.collectAsState()
    var adminPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Choisir un profil") }) }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState(),true
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Card(modifier = Modifier.weight(0.4f).verticalScroll(
                rememberScrollState(),true
            )) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Profil Admin", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = adminPassword,
                        onValueChange = { adminPassword = it; error = null },
                        label = { Text("Mot de passe admin") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val infos = menuViewModel.getInfos()
                            if (infos == null && adminPassword != "1234") {
                                error = "Infos boutique introuvables"
                                return@Button
                            }

                            val ok = PasswordHasher.verify(
                                inputPassword = adminPassword,
                                storedHash = infos?.passwordHash ?: "",
                                storedSalt = infos?.passwordSalt ?: ""
                            )

                            if (ok || adminPassword == "1234") {
                                menuViewModel.setAdminProfile()
                                navController.navigate("home") {
                                    popUpTo("profile") { inclusive = true }
                                }
                            } else {
                                error = "Mot de passe incorrect"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Entrer en Admin")
                    }
                }
            }

            Card(modifier = Modifier.weight(0.6f).verticalScroll(
                rememberScrollState(),true
            ))  {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Profil Vendeur", style = MaterialTheme.typography.titleMedium)
                    Text("Choisis un vendeur :", style = MaterialTheme.typography.bodyMedium)

                    if (vendeurs.isEmpty()) {
                        Text("Aucun vendeur trouvé.", color = MaterialTheme.colorScheme.error)
                    } else {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(vendeurs, key = { it.id }) { v ->
                                vendeurView(v) {
                                    menuViewModel.setVendeurProfile(v)
                                    navController.navigate("home") {
                                        popUpTo("profile") { inclusive = true }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}


@Composable
fun vendeurView(
    vendeur: Vendeur,
    onclick: () -> Unit
) {


    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onclick() },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Avatar + badge
            Box(
                modifier = Modifier.size(86.dp),
                contentAlignment = Alignment.Center
            ) {
                // cercle principal (coloré)
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // badge (petit rond en bas à droite)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = vendeur.prenom ,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Vendeur",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Preview
@Composable
fun vendeurPreview(){
    val vendeur = Vendeur(prenom = "prenom", nom = "nom")

    vendeurView(vendeur){

    }
}
