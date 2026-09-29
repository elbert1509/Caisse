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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.ShopInfos
import com.example.caisse.data.Ticket
import com.example.caisse.data.Vente
import com.example.caisse.data.AppConfig
import com.example.caisse.util.StripAccents
import com.example.caisse.util.formatPrice
import com.example.caisse.util.formatTicketNumber
import com.example.caisse.util.invoiceNoFromId
import com.example.caisse.util.printBitmapEscPos
import com.example.caisse.util.textToBitmap58mm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothAdapter  : BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket : BluetoothSocket? = null
    private var outputStream : OutputStream? = null
    private val _isPrinting = MutableStateFlow(false)
    val isPrinting = _isPrinting.asStateFlow()


    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices = _pairedDevices.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()
    private val ESC = '\u001B'

    // Police très petite (H = 1, W = 1)
    private val FONT_SMALLEST = "$ESC!1"

    // Police normale (reset si besoin)
    private val FONT_RESET = "$ESC!0"

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
    fun printProforma(tableItems: List<Ticket>, total: Double, infos: ShopInfos?, invoiceId: UUID? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1) Reset + taille normale
                writeCmd(0x1B, 0x40)        // ESC @  (initialize)
                writeCmd(0x1D, 0x21, 0x00)  // GS ! 0 (taille normale - largeur/hauteur x1)
                writeCmd(0x1B, 0x45, 0x00)  // ESC E 0 (pas gras)

                // 2) Sélection police plus petite (Font B) pour gagner de la place
                writeCmd(0x1B, 0x4D, 0x01)  // ESC M 1

                val dateHeure = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date())
                val invoiceNo = invoiceId?.let { invoiceNoFromId(it) }
                val sb = StringBuilder()
                sb.append("\u001B\u0061\u0001") // Alignement centré
                sb.append("\r\n")
                sb.append("*** ${infos?.name ?: ""} ***\r\n")
                sb.append("Adresse: ${infos?.address ?: ""}\r\n")
                sb.append("Tel: ${infos?.phone ?: ""}\r\n")
                sb.append("Date: $dateHeure\r\n")
                sb.append(sepLine55())
                sb.append("NOTE PROVISOIRE\r\n")
                sb.append("(Ceci n'est pas un ticket)\r\n")
                sb.append(sepLine55())
                if (invoiceId != null){
                    sb.append("FACTURE CLIENT N°: $invoiceNo\r\n")
                    sb.append(sepLine55())
                }else {
                    sb.append("FACTURE CLIENT \r\n")
                    sb.append(sepLine55())
                }
                sb.append("\u001B\u0061\u0000") // Alignement à gauche

                tableItems.forEach { ticket ->

                    val article = ticket.produit.nom.replace("\n", " ")
                    val qty = ticket.quantity.toString()
                    val price = ticket.produit.prix.toInt().toString()
                    val totalLine = (ticket.produit.prix * ticket.quantity).toInt().toString().replace(" ", "")

                    sb.append(
                        formatItemLine55(
                            article = article,
                            qty = qty,
                            price = price,
                            total = totalLine
                        )
                    )
                }


                sb.append(sepLine55())
                sb.append("TOTAL: ${formatPrice(total, infos?.devise)}\r\n")
                sb.append(sepLine55())
                sb.append("Merci pour votre confiance\r\n")
                sb.append("\u001B\u0061\u0001")
                sb.append("Contact Rody: ${AppConfig.NUMERO}\r\n")
                sb.append("\r\n\r\n\r\n")

                // Remplacement EUR et nettoyage des accents pour la compatibilité POS
                val text = StripAccents(sb.toString().replace("€", "EUR"))
                outputStream?.write(text.toByteArray(Charsets.UTF_8))
                outputStream?.flush()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun printInvoice(
        tableItems: List<Ticket>,
        total: Double,
        infos: ShopInfos?,
        invoiceId: UUID,
        sequenceNumber: Long = 0L,
        signatureHash: String? = null,
        onError: ((Throwable) -> Unit)? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (outputStream == null) {
                    throw IllegalStateException("OutputStream Bluetooth nul")
                }

                writeCmd(0x1B, 0x40)
                writeCmd(0x1D, 0x21, 0x00) // Taille normale
                writeCmd(0x1B, 0x45, 0x00)
                writeCmd(0x1B, 0x4D, 0x01) // Font B (plus petit)

                val dateHeure = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRANCE).format(Date())
                // NF525 Axe B : numéro séquentiel ininterrompu
                val invoiceNo = if (sequenceNumber > 0) formatTicketNumber(sequenceNumber)
                                else invoiceNoFromId(invoiceId)

                // TVA par défaut 20 % pour l'impression (ventilation exacte sur l'écran ticket)
                val montantTVA = total * 5.0 / 120.0
                val montantHT  = total - montantTVA

                val sb = StringBuilder()
                sb.append("\u001B\u0061\u0001") // Alignement centré
                sb.append("\r\n")
                sb.append("*** ${infos?.name ?: ""} ***\r\n")
                sb.append("Adresse: ${infos?.address ?: ""}\r\n")
                sb.append("SIRET: ${infos?.siret ?: "000 000 000"}\r\n")
                sb.append("Tel: ${infos?.phone ?: ""}\r\n")
                sb.append("Date: $dateHeure\r\n")
                sb.append(sepLine55())
                sb.append("TICKET N°: $invoiceNo\r\n")
                sb.append(sepLine55())
                sb.append("\u001B\u0061\u0000") // Alignement à gauche

                tableItems.forEach { ticket ->
                    val article = ticket.produit.nom.replace("\n", " ")
                    val qty = ticket.quantity.toString()
                    val price = formatPriceShort(ticket.produit.prix)
                    val totalLine = formatPriceShort(ticket.produit.prix * ticket.quantity)

                    sb.append(
                        formatItemLine55(
                            article = article,
                            qty = qty,
                            price = price,
                            total = totalLine
                        )
                    )
                }

                sb.append(sepLine55())
                sb.append("TOTAL TTC: ${formatPrice(total, infos?.devise)}\r\n")
                sb.append("TVA (5%): ${formatPrice(montantTVA, infos?.devise)}\r\n")
                sb.append("TOTAL HT : ${formatPrice(montantHT, infos?.devise)}\r\n")

                if (signatureHash != null) {
                    val displayHash = if (signatureHash.length > 8) signatureHash.takeLast(8) else signatureHash
                    sb.append("Signature: $displayHash\r\n")
                }

                sb.append(sepLine55())
                sb.append("${AppConfig.NOM_LOGICIEL} v${AppConfig.VERSION_LOGICIEL}\r\n")
                sb.append("${AppConfig.NUM_CERTIFICAT}\r\n")
                sb.append(sepLine55())
                sb.append("Merci de votre visite !\r\n")
                sb.append("\u001B\u0061\u0001")
                sb.append(" Contact : Rody ${AppConfig.NUMERO} !\r\n")
                sb.append("\r\n\r\n\r\n")

                val text = StripAccents(sb.toString().replace("€", "EUR"))
                outputStream?.write(text.toByteArray(Charsets.UTF_8))
                outputStream?.flush()

                onSuccess?.invoke()
            } catch (e: Exception) {
                Log.e("BluetoothPrint", "Erreur impression", e)
                onError?.invoke(e)
            }
        }
    }
    // Fonction utilitaire pour gagner de la place sur 58mm
    private fun formatPriceShort(amount: Double): String {
        return String.format(Locale.FRANCE, "%.2f", amount)
    }


    private fun formatLine(
        article: String,
        qty: String,
        price: String,
        total: String
    ): String {
        val a = article.take(10).padEnd(10, ' ')
        val q = qty.padStart(4, ' ')
        val p = price.padStart(14, ' ')
        val t = total.padStart(11, ' ')
        return "$a$q$p$t\r\n"
    }

    // Ticket 55mm (printInvoice / printProforma) : largeur RÉELLE mesurée sur l'imprimante
    // = 32 caractères/ligne (identique en Font A et Font B — cette imprimante ignore la
    // commande de sélection de police). Confirmée par une impression de règle physique.
    private val LINE_CHARS_55 = 32
    private fun sepLine55(): String = "-".repeat(LINE_CHARS_55) + "\r\n"

    // Format "reçu" pro sur deux lignes par article : le nom sur sa propre ligne
    // (jamais tronqué au milieu, sauf s'il dépasse vraiment 32 caractères), puis
    // "qte x prix" à gauche et le total de la ligne aligné à droite. Si les deux ne
    // tiennent pas ensemble (gros montants), le total passe sur sa propre ligne
    // plutôt que de déborder et faire un retour à la ligne mal placé.
    private fun formatItemLine55(article: String, qty: String, price: String, total: String): String {
        val name = if (article.length <= LINE_CHARS_55) article else article.take(LINE_CHARS_55)
        val detail = "$qty x $price"
        return if (detail.length + total.length < LINE_CHARS_55) {
            val spaces = LINE_CHARS_55 - detail.length - total.length
            "$name\r\n$detail${" ".repeat(spaces)}$total\r\n"
        } else {
            "$name\r\n$detail\r\n${total.padStart(LINE_CHARS_55, ' ')}\r\n"
        }
    }


    fun testPrint( menuViewModel: MenuViewModel) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 🔹 Nom de l’imprimante connectée
                //val printerName = socket?.remoteDevice?.name ?: "Imprimante inconnue"
                val printerName ="Imprimante inconnue"

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
    private fun writeChunked(bytes: ByteArray, chunkSize: Int = 256) {
        var i = 0
        while (i < bytes.size) {
            val end = minOf(i + chunkSize, bytes.size)
            outputStream?.write(bytes, i, end - i)
            outputStream?.flush()
            Thread.sleep(10) // petit souffle pour les imprimantes fragiles
            i = end
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