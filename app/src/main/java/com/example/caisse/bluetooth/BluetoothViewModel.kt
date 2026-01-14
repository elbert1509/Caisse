package com.example.piece.bluetooth

import android.Manifest
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.piece.data.MenuViewModel
import com.example.piece.data.ShopInfos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.util.UUID
import com.example.piece.data.Ticket
import com.example.piece.util.StripAccents
import com.example.piece.util.formatPrice
import com.example.piece.util.invoiceNoFromId
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BluetoothViewModel : ViewModel() {

    private val bluetoothAdapter  : BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket : BluetoothSocket? = null
    private var outputStream : OutputStream? = null


    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices = _pairedDevices.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()
    private val ESC = '\u001B'

    // Police très petite (H = 1, W = 1)
    private val FONT_SMALLEST = "$ESC!1"

    // Police normale (reset si besoin)
    private val FONT_RESET = "$ESC!0"

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
    private fun writeCmd(vararg bytes: Int) {
        outputStream?.write(bytes.map { it.toByte() }.toByteArray())
    }

    private val CP850: Charset = Charset.forName("CP850")
    fun printInvoice(tableItems: List<Ticket>, total: Double, infos: ShopInfos?, invoiceId: UUID? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1) Reset + taille normale (évite le double-size résiduel)
                writeCmd(0x1B, 0x40)        // ESC @  (initialize)
                writeCmd(0x1D, 0x21, 0x01)  // GS ! 0 (taille normale)
                writeCmd(0x1B, 0x45, 0x00)  // ESC E 0 (pas gras)

                // 2) Sélection police PETITE (Font B)
                writeCmd(0x1B, 0x4D, 0x01)  // ESC M 1

                // 3) Code page pour accents (CP850)
                writeCmd(0x1B, 0x74, 0x02)  // ESC t 2  (souvent = CP850)

                val dateHeure = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date())
                val invoiceNo = invoiceId?.let { invoiceNoFromId(it) }
                val sb = StringBuilder()
                sb.append("\r\n")
                sb.append("*** ${infos?.name ?: ""} ***\r\n")
                sb.append("Adresse: ${infos?.address ?: ""}\r\n")
                sb.append("Tel: ${infos?.phone ?: ""}\r\n")
                sb.append("Date: $dateHeure\r\n")
                sb.append("--------------------------------\r\n")
                if (invoiceId != null) sb.append("FACTURE CLIENT N°: $invoiceNo\r\n")
                sb.append("--------------------------------\r\n")
                sb.append("Article        Qte   Prix    Total\r\n")
                sb.append("--------------------------------\r\n")

                // Colonnes 58mm -> on serre un peu
                tableItems.forEach { ticket ->


                    val name = ticket.produit.nom
                        .replace("\n", " ")
                        .take(17)
                        .padEnd(17, ' ')

                    val qty = ticket.quantity.toString().padStart(3, ' ')
                    val price = String.format("%.2f", ticket.produit.prix).padStart(6, ' ')

                    val lineTotal = formatPrice(
                        ticket.produit.prix * ticket.quantity,
                        infos?.devise
                    )
                        .take(12)
                        .padStart(12, ' ')

                    sb.append("$name $qty $price $lineTotal\r\n")
                }

                sb.append("--------------------------------\r\n")
                sb.append("TOTAL: ${formatPrice(total, infos?.devise)}\r\n")
                sb.append("--------------------------------\r\n")
                sb.append("Merci pour votre confiance\r\n")
                sb.append("\r\n\r\n\r\n")

                // IMPORTANT: encoder le texte avec la même code page
                val text = StripAccents(sb.toString())
                outputStream?.write(text.toByteArray(Charsets.US_ASCII))
                outputStream?.flush()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }



    fun testPrint(context: Context, menuViewModel: MenuViewModel) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 🔹 Nom de l’imprimante connectée
                val printerName = socket?.remoteDevice?.name ?: "Imprimante inconnue"

                // 🔹 Infos magasin
                val infos = menuViewModel.getInfos()
                val shopName = infos?.name ?: "Mon Magasin"

                val sb = StringBuilder()

                sb.append("\n")
                sb.append("************************\r\n")
                sb.append("        TEST PRINT       \r\n")
                sb.append("************************\r\n")
                sb.append("\r\n")
                sb.append("Magasin    : $shopName\r\n")
                sb.append("Imprimante : $printerName\r\n")
                sb.append("\r\n")
                sb.append("------------------------\r\n")
                sb.append("Connexion OK ✅\r\n")
                sb.append("Bluetooth fonctionnel\r\n")
                sb.append("------------------------\r\n")
                sb.append("\r\n")
                sb.append("Test d'impression réussi 👍\r\n")
                sb.append("\r\n\r\n\r\n") // Avance papier

                outputStream?.write(sb.toString().toByteArray(Charsets.UTF_8))
                outputStream?.flush()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        fun provideFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(BluetoothViewModel::class.java)) {
                    return BluetoothViewModel() as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }



}