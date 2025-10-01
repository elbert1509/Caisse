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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.caisse.MainActivity

@Composable
fun ParametreBluetooothScreen(viewModel: BluetoothViewModel, navController: NavController) {
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()

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


    Column(Modifier.padding(20.dp)) {
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
                Text("Déconnecter")
            }
        }
    }
}