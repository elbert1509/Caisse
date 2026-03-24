package com.example.caisse.composable

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.caisse.data.CaisseDataBase
import com.example.caisse.data.MenuViewModel
import com.example.caisse.model.AuthViewModel
import com.example.caisse.ui.theme.MintEnd
import com.example.caisse.ui.theme.MintStart
import com.example.caisse.ui.theme.Slate100
import com.example.caisse.ui.theme.Slate500
import com.example.caisse.ui.theme.Slate700
import com.example.caisse.ui.theme.Slate900
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    vm: AuthViewModel,
    onSignedInNavigateRoute: String = "home", // adapte au nom de ta route Dashboard
    menuViewModel: MenuViewModel
) {
    val ui by vm.ui.collectAsState()
    var isSignUp by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    // Redirige si connecté
    if (ui.isSignedIn) {
        LaunchedEffect(ui.isSignedIn) {
            if (ui.isSignedIn) {
                val ctx = navController.context
                val prefs = ctx.getSharedPreferences("sync", Context.MODE_PRIVATE)
                val uid = Firebase.auth.currentUser?.uid
                val storedUid = prefs.getString("uid", null)

                // ⚙️ Si un autre utilisateur se connecte, ou premier lancement
                if (uid != null && uid != storedUid) {
                    // 🔄 vider la base locale (en thread IO)
                    withContext(Dispatchers.IO) {
                        CaisseDataBase.getDatabase(ctx).clearAllTables()
                    }

                    // ♻️ réinitialiser les infos de sync
                    prefs.edit()
                        .putString("uid", uid)
                        .putLong("lastSyncAt", 0L) // ⚠️ force initial sync
                        .apply()
                }

                // Lance la sync APRES le wipe
                vm.enqueueSync(context = ctx, tag = "sync")
                menuViewModel.loggerEvenement(
                    type = "Connexion",
                    description = "UID=$uid"
                )

                // Démarre la surveillance temps réel des tables

                if (uid != null) {
                   // menuViewModel.startRealtimeTables(uid)
                }

                // Puis navigation
                navController.navigate(onSignedInNavigateRoute) {
                    popUpTo("login") { inclusive = true }
                    launchSingleTop = true
                }
            }
        }

    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isSignUp) "Créer un compte" else "Connexion",
                            style = MaterialTheme.typography.titleLarge,
                            color = Slate900
                        )
                        Text(
                            text = "Accédez à vos données et synchronisation",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Slate100
    ) {padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.padding(16.dp)) {

                OutlinedTextField(
                    value = ui.email,
                    onValueChange = vm::updateEmail,
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = ui.password,
                    onValueChange = vm::updatePassword,
                    label = { Text("Mot de passe") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Text(
                            text = if (showPassword) "Masquer" else "Voir",
                            color = Slate500,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showPassword = !showPassword }
                                .padding(6.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (ui.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(ui.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    menuViewModel.loggerEvenement("Erreur", ui.error!!)
                }

                Spacer(Modifier.height(16.dp))

                GradientButton(
                    text = if (isSignUp) "Créer le compte" else "Se connecter",
                    loading = ui.isLoading
                ) {
                    if (isSignUp) vm.signUp() else vm.signIn()
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    Text(
                        text = if (isSignUp) "Déjà inscrit ?" else "Pas de compte ?",
                        color = Slate700,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isSignUp) "Se connecter" else "Créer un compte",
                        color = Slate900,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            //.clickable { isSignUp = !isSignUp }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        }

    }


@Composable
private fun GradientButton(text: String, loading: Boolean, onClick: () -> Unit) {
    val gradient = Brush.linearGradient(listOf(MintStart, MintEnd))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(gradient)
            .clickable(enabled = !loading) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = Color.White,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(text, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}