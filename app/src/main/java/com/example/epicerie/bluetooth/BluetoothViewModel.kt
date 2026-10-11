package com.example.epicerie.bluetooth

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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.epicerie.data.MenuViewModel
import com.example.epicerie.data.ShopInfos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.util.UUID
import com.example.epicerie.data.Ticket
import com.example.epicerie.util.StripAccents
import com.example.epicerie.util.formatPrice
import com.example.epicerie.util.invoiceNoFromId
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothAdapter  : BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket : BluetoothSocket? = null
    private var outputStream : OutputStream? = null


    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices = _pairedDevices.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()
    private val ESC = '\u001B'

    // ---- Reconnexion automatique au dernier appareil Bluetooth appairé ----
    // Évite de devoir repasser par Paramètres > Bluetooth à chaque lancement de l'app.
    private val btPrefs = application.getSharedPreferences("bluetooth_prefs", Context.MODE_PRIVATE)
    private val KEY_LAST_DEVICE_ADDRESS = "last_device_address"

    init {
        tryAutoConnect()
    }

    private fun tryAutoConnect() {
        val ctx = getApplication<Application>()
        val savedAddress = btPrefs.getString(KEY_LAST_DEVICE_ADDRESS, null) ?: return

        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else true
        if (!hasPermission) return

        try {
            val device = bluetoothAdapter?.bondedDevices?.firstOrNull { it.address == savedAddress }
            if (device != null) {
                connecToDevice(device, ctx)
            }
        } catch (e: SecurityException) {
            Log.e("BluetoothViewModel", "Permission manquante pour la reconnexion automatique", e)
        }
    }

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
                btPrefs.edit().putString(KEY_LAST_DEVICE_ADDRESS, device.address).apply()
            } catch (e: SecurityException) {
                e.printStackTrace()
                closeFailedSocket()
                _isConnected.value = false
            } catch (e: Exception) {
                e.printStackTrace()
                closeFailedSocket()
                _isConnected.value = false
            }
        }
    }

    // Referme proprement une connexion ratée : sans ça, un socket/outputStream à moitié
    // initialisé reste en mémoire et peut faire échouer silencieusement la prochaine
    // tentative (manuelle ou reconnexion auto au démarrage).
    private fun closeFailedSocket() {
        try {
            socket?.close()
        } catch (_: Exception) {
        } finally {
            socket = null
            outputStream = null
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
                // 1) Reset + taille normale. ⚠️ GS ! 0x01 mettait le texte en double-hauteur
                // (bit0 = hauteur x2) malgré le commentaire "taille normale" — ça consommait
                // deux fois plus de papier que nécessaire sur chaque ligne. 0x00 = vraie taille normale.
                writeCmd(0x1B, 0x40)        // ESC @  (initialize)
                writeCmd(0x1D, 0x21, 0x00)  // GS ! 0 (taille normale, pas de double-hauteur)
                writeCmd(0x1B, 0x45, 0x00)  // ESC E 0 (pas gras)

                // 2) Sélection police PETITE (Font B)
                writeCmd(0x1B, 0x4D, 0x01)  // ESC M 1

                // 3) Code page pour accents (CP850)
                writeCmd(0x1B, 0x74, 0x02)  // ESC t 2  (souvent = CP850)

                val dateHeure = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date())
                val invoiceNo = invoiceId?.let { invoiceNoFromId(it) }
                val sb = StringBuilder()
                sb.append("*** ${infos?.name ?: ""} ***\r\n")
                sb.append("Adresse: ${infos?.address ?: ""}\r\n")
                sb.append("Tel: ${infos?.phone ?: ""}\r\n")
                sb.append("Date: $dateHeure\r\n")
                sb.append(sepLine55())
                if (invoiceId != null) sb.append("FACTURE CLIENT N°: $invoiceNo\r\n")
                sb.append(sepLine55())

                // Nom du produit sur sa propre ligne (jamais tronqué au milieu d'un mot comme
                // avec l'ancien format 4-colonnes), puis qté x prix / total aligné à droite.
                tableItems.forEach { ticket ->
                    val price = String.format(Locale.FRANCE, "%.2f", ticket.produit.prix)
                    val lineTotal = String.format(
                        Locale.FRANCE, "%.2f", ticket.produit.prix * ticket.quantity
                    )
                    sb.append(
                        formatItemLine55(
                            article = ticket.produit.nom,
                            qty = ticket.quantity.toString(),
                            price = price,
                            total = lineTotal
                        )
                    )
                }

                sb.append(sepLine55())
                sb.append("TOTAL: ${formatPrice(total, infos?.devise)}\r\n")
                sb.append(sepLine55())
                sb.append("Merci pour votre confiance\r\n")
                sb.append("\r\n\r\n") // juste assez d'avance papier pour déchirer le ticket

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

    // Ticket (printInvoice) : 32 caractères/ligne = valeur par défaut sûre pour une imprimante
    // thermique 55/58mm, PAS mesurée sur l'imprimante réelle de ce déploiement (contrairement
    // à v2_prod_v16 où elle a été confirmée par un test physique). À ajuster si le ticket
    // déborde ou laisse trop de marge — voir la méthode utilisée sur v2_prod_v16 (impression
    // d'une règle de caractères) si besoin de mesurer précisément.
    private val LINE_CHARS_55 = 32
    private fun sepLine55(): String = "-".repeat(LINE_CHARS_55) + "\r\n"

    private fun formatItemLine55(article: String, qty: String, price: String, total: String): String {
        val name = article.replace("\n", " ").let {
            if (it.length <= LINE_CHARS_55) it else it.take(LINE_CHARS_55)
        }
        val detail = "$qty x $price"
        return if (detail.length + total.length < LINE_CHARS_55) {
            val spaces = LINE_CHARS_55 - detail.length - total.length
            "$name\r\n$detail${" ".repeat(spaces)}$total\r\n"
        } else {
            "$name\r\n$detail\r\n${total.padStart(LINE_CHARS_55, ' ')}\r\n"
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(BluetoothViewModel::class.java)) {
                    return BluetoothViewModel(application) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }



}