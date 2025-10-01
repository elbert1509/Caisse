package com.example.caisse.bluetooth

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.util.UUID
import com.example.caisse.data.Ticket

class BluetoothViewModel : ViewModel() {

    private val bluetoothAdapter  : BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket : BluetoothSocket? = null
    private var outputStream : OutputStream? = null


    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices = _pairedDevices.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun loadPairedDevices() {
        bluetoothAdapter?.bondedDevices?.let {
            _pairedDevices.value = it.toList()
        }
    }


    fun connecToDevice(device: BluetoothDevice, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {   // 🚀 tourne sur un thread background
            Log.d("BluetoothViewModel", " Logfg Connected to device: ${device.name}")
            try {
                val uuid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                    val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
                    Log.d("BluetoothViewModel", " device uuids : ${device.uuids} hasPermission : $hasPermission")
                    if (hasPermission) device.uuids?.firstOrNull()?.uuid else null
                } else {
                    Log.d("BluetoothViewModel", " Logfg Connected to device: ${device.name} with ${device.address}")
                    device.uuids?.firstOrNull()?.uuid
                } ?: UUID.fromString("00001101-0000-1000-8000-00805f9b34fb") // UUID SPP
                    Log.d("BluetoothViewModel", " device uuid : $uuid has Permission : ${device.uuids}")
                socket = device.createRfcommSocketToServiceRecord( UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"))
                bluetoothAdapter?.cancelDiscovery()
                socket?.connect()   // ✅ safe car exécuté en I/O thread
                outputStream = socket?.outputStream

                _isConnected.value = true
            } catch (e: SecurityException) {
                e.printStackTrace()
                _isConnected.value = false
            } catch (e: Exception) {
                e.printStackTrace()
                _isConnected.value = false
            }
        }
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            socket = null
            outputStream = null
            _isConnected.value = false
        }
    }
    fun printText(text: String) {
        viewModelScope.launch(Dispatchers.IO) {   // 🔄 en background
            try {
                outputStream?.write(text.toByteArray())
                outputStream?.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun printInvoice(tableItems: List<Ticket>, total: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sb = StringBuilder()

                // --- En-tête ---
                sb.appendln("************ COMME DES GARÇONS ************")
                sb.appendln("Adresse: 123 Rue Exemple, Paris")
                sb.appendln("Tel: 01 23 45 67 89")
                sb.appendln("-------------------------------------------")
                sb.appendln("               FACTURE CLIENT              ")
                sb.appendln("-------------------------------------------")
                sb.appendln("Article               Qté   PU     Total")
                sb.appendln("-------------------------------------------")

                // --- Détail des articles ---
                tableItems.forEach { ticket ->
                    val name = ticket.produit.nom.padEnd(18, ' ').take(18)
                    val qty = ticket.quantity.toString().padStart(3, ' ')
                    val price = String.format("%.2f", ticket.produit.prix).padStart(6, ' ')
                    val lineTotal = String.format("%.2f", ticket.produit.prix * ticket.quantity).padStart(7, ' ')
                    sb.appendln("$name $qty  $price  $lineTotal")
                }

                sb.appendln("-------------------------------------------")
                sb.appendln(String.format("TOTAL:%36.2f €", total))
                sb.appendln("-------------------------------------------")
                sb.appendln("      Merci pour votre confiance 🙏")
                sb.appendln("*******************************************")
                sb.appendln("\n\n\n") // Avance papier

                val text = sb.toString()
                outputStream?.write(text.toByteArray(Charsets.UTF_8))
                outputStream?.flush()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


}