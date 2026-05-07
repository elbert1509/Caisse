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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.caisse.ui.theme.Accent500
import com.example.caisse.ui.theme.SemanticGreen
import com.google.firebase.auth.auth

// ---------- Permission helpers ----------

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
    try { action() } catch (_: SecurityException) {}
}

// ---------- Screen ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParametreBluetooothScreen(
    viewModel: BluetoothViewModel,
    navController: NavController,
    authVm: AuthViewModel,
    menuViewModel: MenuViewModel
) {
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val isConnected   by viewModel.isConnected.collectAsState()
    val info = menuViewModel.getInfos()

    var showLogoutDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            launcher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN))
        }
    }

    val context = LocalContext.current
    val hasPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.BLUETOOTH_CONNECT
    ) == PackageManager.PERMISSION_GRANTED

    val user  = com.google.firebase.Firebase.auth.currentUser
    val email = user?.email ?: "Utilisateur non connecté"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.parametre),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (info?.name != null) {
                            Text(
                                info.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.Retour))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (!hasAllBtPermissions(context)) launcher.launch(requiredBtPerms())
                            else safeRun { viewModel.loadPairedDevices() }
                        }
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.Actualiser))
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Filled.Logout, contentDescription = stringResource(R.string.Sedeconnecter))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor        = MaterialTheme.colorScheme.primary,
                    titleContentColor     = Color.White,
                    actionIconContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            // ================================================================
            // COMPTE
            // ================================================================
            SectionLabel("COMPTE")
            Spacer(Modifier.height(4.dp))

            SettingsCard {
                // Avatar + identité
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape  = CircleShape,
                        color  = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text  = email.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            email,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Firebase Auth",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Statut Bluetooth
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Imprimante",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StatusBadge(isConnected)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ================================================================
            // IMPRIMANTE BLUETOOTH
            // ================================================================
            SectionLabel("IMPRIMANTE BLUETOOTH")
            Spacer(Modifier.height(4.dp))

            SettingsCard {
                // En-tête de la section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBox(icon = Icons.Filled.Print, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.imprimante_bluetooth),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            stringResource(R.string.text_selection_app),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            if (!hasAllBtPermissions(context)) launcher.launch(requiredBtPerms())
                            else safeRun { viewModel.loadPairedDevices() }
                        }
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.Actualiser),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                HorizontalDivider()

                // Liste des appareils
                if (pairedDevices.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.BluetoothDisabled,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            stringResource(R.string.text_aucun_app_trou),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    pairedDevices.forEachIndexed { index, device ->
                        if (index > 0) HorizontalDivider(modifier = Modifier.padding(start = 70.dp))
                        DeviceRow(
                            device = device,
                            isConnected = isConnected,
                            hasPermission = hasPermission,
                            onClick = {
                                if (!hasAllBtPermissions(context)) launcher.launch(requiredBtPerms())
                                else safeRun { viewModel.connecToDevice(device, context) }
                            }
                        )
                    }
                }

                // Boutons d'action (si connecté)
                if (isConnected) {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { safeRun { viewModel.testPrint(menuViewModel) } },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Filled.Print, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.text_test_impres))
                        }
                        OutlinedButton(
                            onClick = { safeRun { viewModel.disconnect() } },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Filled.BluetoothDisabled, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.text_deconnecter))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ================================================================
            // PARAMÈTRES
            // ================================================================
            SectionLabel("PARAMÈTRES")
            Spacer(Modifier.height(4.dp))

            SettingsCard {
                SettingItem(
                    icon     = Icons.Filled.Sync,
                    iconTint = SemanticGreen,
                    title    = stringResource(R.string.text_synch),
                    subtitle = "Envoyer les données vers le cloud",
                    onClick  = { authVm.enqueueSync(context = context, tag = "sync") }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 70.dp))
                SettingItem(
                    icon     = Icons.Filled.Store,
                    iconTint = Accent500,
                    title    = "Informations boutique",
                    subtitle = "Nom, adresse, SIRET, devise…",
                    onClick  = { navController.navigate("infosStart") }
                )
            }

            Spacer(Modifier.height(12.dp))

            // ================================================================
            // LANGUE
            // ================================================================
            SectionLabel(stringResource(R.string.text_langue).uppercase())
            Spacer(Modifier.height(4.dp))

            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBox(icon = Icons.Filled.Language, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(14.dp))
                    Text(
                        "Langue / Language",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Segmented control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 14.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    listOf("Français" to "fr", "English" to "en").forEachIndexed { i, (label, code) ->
                        val selected = getCurrentLanguage() == code
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { setAppLocale(context, code) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (selected) Color.White
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                        if (i == 0) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(44.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        // ================================================================
        // Dialog déconnexion
        // ================================================================
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                icon = { Icon(Icons.Filled.Logout, null, tint = MaterialTheme.colorScheme.error) },
                title = { Text(stringResource(R.string.text_confirmer_decon)) },
                text  = { Text(stringResource(R.string.text_confirmatio_dec)) },
                confirmButton = {
                    Button(
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
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.text_deconnexion))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text(stringResource(R.string.text_annuler))
                    }
                }
            )
        }
    }
}

// ---------- UI Components ----------

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape     = RoundedCornerShape(16.dp)
    ) {
        content()
    }
}

@Composable
private fun IconBox(icon: ImageVector, tint: Color) {
    Surface(
        shape    = RoundedCornerShape(10.dp),
        color    = tint.copy(alpha = 0.12f),
        modifier = Modifier.size(40.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun StatusBadge(isConnected: Boolean) {
    val bgColor   = if (isConnected) SemanticGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
    val textColor = if (isConnected) SemanticGreen else MaterialTheme.colorScheme.error
    val icon      = if (isConnected) Icons.Filled.BluetoothConnected else Icons.Filled.BluetoothDisabled
    val label     = if (isConnected) stringResource(R.string.tile_appareil_connecte) else stringResource(R.string.tile_aucun_connecte)

    Surface(shape = RoundedCornerShape(50), color = bgColor) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, null, tint = textColor, modifier = Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = textColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DeviceRow(
    device: BluetoothDevice,
    isConnected: Boolean,
    hasPermission: Boolean,
    onClick: () -> Unit
) {
    val deviceName = try {
        if (hasPermission) device.name ?: "Appareil inconnu" else "Nom indisponible"
    } catch (_: SecurityException) { "Nom indisponible" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                if (isConnected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape    = CircleShape,
            color    = if (isConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                       else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(40.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    if (isConnected) Icons.Filled.BluetoothConnected else Icons.Filled.Bluetooth,
                    contentDescription = null,
                    tint = if (isConnected) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(deviceName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(device.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (isConnected) {
            Surface(shape = RoundedCornerShape(50), color = SemanticGreen.copy(alpha = 0.12f)) {
                Text(
                    "Connecté",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = SemanticGreen,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        } else {
            Icon(
                Icons.Filled.KeyboardArrowRight,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBox(icon = icon, tint = iconTint)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
    }
}

// ---------- Helpers (backward-compat, gardés publics) ----------

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
            contentColor   = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape    = RoundedCornerShape(12.dp),
        modifier = Modifier.width(140.dp)
    ) {
        Text(text)
    }
}

fun setAppLocale(context: Context, language: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(LocaleManager::class.java).applicationLocales =
            LocaleList.forLanguageTags(language)
    } else {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
    }
}
