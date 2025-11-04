package com.example.caisse.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.room.util.TableInfo
import androidx.work.WorkManager
import com.example.caisse.data.MenuViewModel
import com.example.caisse.model.AuthViewModel
import com.google.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParametreBluetooothScreen(viewModel: BluetoothViewModel, navController: NavController,authVm: AuthViewModel, menuViewModel: MenuViewModel) {
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Callback : permissions accordées ou non
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            launcher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        }
    }
    val context = LocalContext.current
    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.BLUETOOTH_CONNECT
    ) == PackageManager.PERMISSION_GRANTED

    val user = com.google.firebase.Firebase.auth.currentUser
    val email = user?.email ?: "Utilisateur non connecté"
    Scaffold(
        topBar = {
            TopAppBar(
                title = {   },
                navigationIcon = {
                    Row (modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {

                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                        }
                        Text(email)
                        IconButton(onClick = {  showLogoutDialog = true }) {
                            Icon(Icons.Filled.Logout, contentDescription = "Quitter ")

                        }
                    }

                }
            )
        }
    ) { padding ->

        Column(Modifier.padding(padding)) {
            Spacer(Modifier.height(12.dp))
            Text("Sélectionner une imprimante Bluetooth", style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(12.dp))

            Button(onClick = { viewModel.loadPairedDevices() }) {
                Text("Actualiser")
            }
            pairedDevices.forEach { device: BluetoothDevice ->
                val deviceName = if (hasPermission) device.name else "Nom indisponible"

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (hasPermission){
                                viewModel.connecToDevice(device, navController.context)
                                Log.d("BluetoothViewModel", "Connecting to device: $deviceName with ${device.address}")
                            }
                        }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(deviceName ?: "Appareil inconnu")
                    if (isConnected ) {
                        Text("✅", style = MaterialTheme.typography.titleLarge)
                    }else{
                        Text("❌", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }

            if (isConnected) {
                Button(onClick = { viewModel.disconnect() }) {
                    Text("Déconnecter l'appareil")
                }
            }

            Button(
                onClick = {
                    authVm.enqueueSync(
                        context = context,
                        tag = "sync"
                    )
                }

            )
            {
                Column (verticalArrangement = Arrangement.Center, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally){
                    Icon(Icons.Filled.Sync, contentDescription = "Synchroniser")
                    Text("Synchroniser les données")
                }

            }
            if (showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showLogoutDialog = false
                                authVm.enqueueSync(
                                    context = context,
                                    tag = "sync"
                                )
                                authVm.signOut();
                                try {
                                    menuViewModel.clearCart()
                                    menuViewModel.clearTableItems()
                                } catch (_: Exception) { /* no-op si pas dispo ici */ }

                                if (isConnected) viewModel.disconnect()
                                try { WorkManager.getInstance(context).cancelAllWorkByTag("sync") } catch (_: Exception) {}
                                navController.navigate("login"){
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        ) {
                            Text("Déconnexion")
                        }
                    },
                    dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Annuler") } },
                    title = { Text("Confirmer") },
                    text  = { Text("Voulez-vous vous déconnecter ?") }

                )
            }
        }
    }


}