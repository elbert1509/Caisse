package com.example.caisse.bluetooth

import android.Manifest
import android.app.LocaleManager
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.LocaleList
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavController
import androidx.work.WorkManager
import com.example.caisse.R
import com.example.caisse.data.MenuViewModel
import com.example.caisse.model.AuthViewModel
import com.google.firebase.auth.auth


private fun requiredBtPerms(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
    else
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

private fun hasAllBtPermissions(ctx: android.content.Context): Boolean =
    requiredBtPerms().all {
        ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED
    }

private fun safeRun(action: () -> Unit) {
    try { action() } catch (_: SecurityException) { /* ignore/log if needed */ }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParametreBluetooothScreen(viewModel: BluetoothViewModel, navController: NavController,authVm: AuthViewModel, menuViewModel: MenuViewModel) {
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val info  = menuViewModel.getInfos()

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
                title = {
                    Text(
                         stringResource(R.string.parametre) + ":  "+ "${info?.name}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.Retour))
                    }
                },
                actions = {
                    // Scan/Refresh
                    IconButton(
                        onClick = {
                            if (!hasAllBtPermissions(context)) {
                                launcher.launch(requiredBtPerms())
                            } else {
                                safeRun { viewModel.loadPairedDevices() }
                            }
                        }
                    ) { Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.Actualiser)) }

                    // Logout
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Filled.Logout, contentDescription = stringResource(R.string.Sedeconnecter))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),

        ) {

            // ==== Carte : Compte & statut ====
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    AccentBar()
                    Spacer(Modifier.height(12.dp))

                    Text(stringResource(R.string.tile_compte_connecte), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        email ,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(
                            if (isConnected) Icons.Filled.BluetoothConnected else Icons.Filled.BluetoothDisabled,
                            contentDescription = null,
                            tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Text(
                            if (isConnected) stringResource(R.string.tile_appareil_connecte)  else  stringResource(R.string.tile_aucun_connecte),
                            color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // ==== Carte : Imprimante Bluetooth (appareils jumelés) ====
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    AccentBar()
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(stringResource(R.string.imprimante_bluetooth), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                stringResource(R.string.text_selection_app),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                if (!hasAllBtPermissions(context)) {
                                    launcher.launch(requiredBtPerms())
                                } else {
                                    safeRun { viewModel.loadPairedDevices() }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Refresh, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.Actualiser))
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (pairedDevices.isEmpty()) {
                        Text(stringResource(R.string.text_aucun_app_trou), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            pairedDevices.forEach { device: BluetoothDevice ->
                                val deviceName = try {
                                    if (hasAllBtPermissions(context)) device.name else "Nom indisponible"
                                } catch (_: SecurityException) { stringResource(R.string.text_nom_indispo) }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (!hasAllBtPermissions(context)) {
                                                launcher.launch(requiredBtPerms())
                                            } else {
                                                safeRun { viewModel.connecToDevice(device, context) }
                                            }
                                        }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(deviceName ?: stringResource(R.string.text_appareil_inconnu), fontWeight = FontWeight.SemiBold)
                                        Text(
                                            device.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isConnected && pairedDevices.any { it.address == device.address }) {
                                        Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                    } else {
                                        Icon(Icons.Filled.Bluetooth, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }

                    if (isConnected) {
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { safeRun { viewModel.testPrint(context,menuViewModel) } }
                            ) { Icon(Icons.Filled.Print, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.text_test_impres)) }

                            OutlinedButton(
                                onClick = { safeRun { viewModel.disconnect() } }
                            ) { Icon(Icons.Filled.BluetoothDisabled, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.text_deconnecter)) }
                        }
                    }
                }
            }

            // ==== Carte : Actions rapides ====
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    AccentBar()
                    Spacer(Modifier.height(12.dp))

                    Text("Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ActionButton(Icons.Filled.Sync, stringResource(R.string.text_synch)) {
                            authVm.enqueueSync(context = context, tag = "sync")
                        }
                        ActionButton(Icons.Filled.Info, "Infos") {
                            navController.navigate("infos")
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    AccentBar()
                    Spacer(Modifier.height(12.dp))

                    Text(stringResource(R.string.text_langue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    SettingsSection(title = "Langue / Language") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            LanguageButton(
                                text = "Français",
                                code = "fr",
                                isSelected = getCurrentLanguage() == "fr",
                                onClick = { setAppLocale(context, "fr") }
                            )
                            LanguageButton(
                                text = "English",
                                code = "en",
                                isSelected = getCurrentLanguage() == "en",
                                onClick = { setAppLocale(context, "en") }
                            )
                        }
                    }
                }
            }
        }

        // ==== Dialog de déconnexion ====
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            authVm.enqueueSync(context = context, tag = "sync")
                            authVm.signOut()
                            try {
                                menuViewModel.clearCart()
                                menuViewModel.clearTableItems()
                                menuViewModel.stopRealtimeTables()
                                WorkManager.getInstance(context).cancelAllWorkByTag("sync")
                            } catch (_: Exception) {}
                            if (isConnected) safeRun { viewModel.disconnect() }
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    ) { Text(stringResource(R.string.text_deconnexion)) }
                },
                dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text(stringResource(R.string.text_annuler)) } },
                title = { Text(stringResource(R.string.text_confirmer_decon)) },
                text = { Text(stringResource(R.string.text_confirmatio_dec)) }
            )
        }
    }
}

// ---------- UI helpers ----------
@Composable
private fun AccentBar() {
    Box(
        modifier = Modifier
            .height(6.dp)
            .fillMaxWidth(0.35f)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                ),
                shape = RoundedCornerShape(50)
            )
    )
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            tonalElevation = 4.dp,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(14.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

fun getCurrentLanguage(): String {
    val locales = AppCompatDelegate.getApplicationLocales()
    return if (!locales.isEmpty) {
        locales[0]?.language ?: "fr"
    } else {
        LocaleListCompat.getDefault()[0]?.language ?: "fr"
    }
}
@Composable
fun LanguageButton(text: String, code: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.width(140.dp)
    ) {
        Text(text)
    }
}

/**
 * Change la langue de l'application dynamiquement.
 * Nécessite que MainActivity hérite de AppCompatActivity.
 */
fun setAppLocale(context: Context, language: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(LocaleManager::class.java).applicationLocales =
            LocaleList.forLanguageTags(language)
    } else {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
    }
}

