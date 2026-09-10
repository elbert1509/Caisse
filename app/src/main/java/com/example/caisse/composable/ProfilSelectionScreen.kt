package com.example.caisse.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.ShopInfos
import com.example.caisse.data.Vendeur
import com.example.caisse.model.AuthViewModel
import com.example.caisse.session.CurrentUserViewModel
import com.example.caisse.ui.theme.Brand600
import com.example.caisse.ui.theme.Slate700
import com.example.caisse.util.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilSelectionScreen(
    navController: NavController,
    menuViewModel: MenuViewModel,
    currentUserViewModel: CurrentUserViewModel,
    authViewModel: AuthViewModel
) {
    val vendeurs by menuViewModel.vendeurs.collectAsState()
    val infos by menuViewModel.observeInfos().collectAsState(initial = null)
    val ctx = androidx.compose.ui.platform.LocalContext.current

    // Un vendeur ajouté ou un PIN redéfini sur un autre appareil doit apparaître ici sans que
    // l'utilisateur ait à forcer une synchro manuellement.
    LaunchedEffect(Unit) {
        authViewModel.enqueueSyncUnique(context = ctx)
    }

    var vendeurPourPin by remember { mutableStateOf<Vendeur?>(null) }
    var demanderMotDePasseGerant by remember { mutableStateOf(false) }

    fun entrerDansApp() {
        navController.navigate("home") {
            popUpTo("profil") { inclusive = true }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Qui êtes-vous ?") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(vendeurs) { vendeur ->
                    ProfilTile(
                        title = "${vendeur.prenom} ${vendeur.nom}",
                        icon = Icons.Filled.Person,
                        color = Brand600,
                        onClick = { vendeurPourPin = vendeur }
                    )
                }
                item {
                    ProfilTile(
                        title = "Gérant",
                        icon = Icons.Filled.AdminPanelSettings,
                        color = Slate700,
                        onClick = { demanderMotDePasseGerant = true }
                    )
                }
            }
        }
    }

    vendeurPourPin?.let { vendeur ->
        PinDialog(
            vendeur = vendeur,
            onDismiss = { vendeurPourPin = null },
            onSuccess = {
                currentUserViewModel.loginAsVendeur(vendeur)
                vendeurPourPin = null
                entrerDansApp()
            }
        )
    }

    if (demanderMotDePasseGerant) {
        MotDePasseGerantDialog(
            infos = infos,
            onDismiss = { demanderMotDePasseGerant = false },
            onSuccess = {
                currentUserViewModel.loginAsGerant()
                demanderMotDePasseGerant = false
                entrerDansApp()
            }
        )
    }
}

@Composable
private fun ProfilTile(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = cardElevation(4.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun PinDialog(
    vendeur: Vendeur,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${vendeur.prenom} ${vendeur.nom}") },
        text = {
            Column {
                Text("Saisissez votre code PIN à 4 chiffres.")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) { pin = it; error = null } },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = pin.length == 4 && !isChecking,
                onClick = {
                    if (vendeur.pinHash.isEmpty()) {
                        error = "Aucun PIN défini pour ce vendeur. Contactez le gérant."
                        return@Button
                    }
                    isChecking = true
                    scope.launch {
                        val ok = withContext(Dispatchers.Default) {
                            PasswordHasher.verify(pin, vendeur.pinHash, vendeur.pinSalt)
                        }
                        isChecking = false
                        if (ok) onSuccess() else { error = "PIN incorrect."; pin = "" }
                    }
                }
            ) {
                if (isChecking) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Valider")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun MotDePasseGerantDialog(
    infos: ShopInfos?,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var pwd by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mot de passe Gérant") },
        text = {
            Column {
                OutlinedTextField(
                    value = pwd,
                    onValueChange = { pwd = it; error = null },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = pwd.isNotBlank() && !isChecking,
                onClick = {
                    if (infos == null) {
                        error = "Configuration boutique introuvable."
                        return@Button
                    }
                    isChecking = true
                    scope.launch {
                        val ok = withContext(Dispatchers.Default) {
                            PasswordHasher.verify(pwd, infos.passwordHash, infos.passwordSalt)
                        }
                        isChecking = false
                        if (ok) onSuccess() else { error = "Mot de passe incorrect."; pwd = "" }
                    }
                }
            ) {
                if (isChecking) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Entrer")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
