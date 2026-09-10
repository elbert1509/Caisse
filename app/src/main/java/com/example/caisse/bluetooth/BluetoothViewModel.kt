package com.example.caisse.bluetooth

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
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
import woyou.aidlservice.jiuiv5.IWoyouService
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

    // ---- Imprimante intégrée au terminal (Sunmi & compatibles "woyou") ----
    // Certains terminaux POS Android (ex: Sunmi V2) ont leur imprimante thermique intégrée,
    // pilotée par un service système local — pas une imprimante Bluetooth externe appairée.
    // On la détecte au démarrage ; si présente, on imprime via elle en priorité (colonnes
    // parfaitement alignées via printColumnsString, pas de pairage Bluetooth nécessaire).
    private val SUNMI_SERVICE_PACKAGE = "woyou.aidlservice.jiuiv5"
    private val SUNMI_SERVICE_ACTION = "woyou.aidlservice.jiuiv5.IWoyouService"
    private var sunmiService: IWoyouService? = null
    private val _sunmiAvailable = MutableStateFlow(false)
    val sunmiAvailable = _sunmiAvailable.asStateFlow()

    private val sunmiServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            sunmiService = IWoyouService.Stub.asInterface(service)
            _sunmiAvailable.value = true
            Log.d("BluetoothViewModel", "Imprimante intégrée connectée")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            sunmiService = null
            _sunmiAvailable.value = false
        }
    }

    // ---- Reconnexion automatique au dernier appareil Bluetooth appairé ----
    // Évite de devoir repasser par Paramètres > Bluetooth à chaque lancement de l'app.
    private val btPrefs = application.getSharedPreferences("bluetooth_prefs", Context.MODE_PRIVATE)
    private val KEY_LAST_DEVICE_ADDRESS = "last_device_address"

    init {
        tryBindImprimanteIntegree()
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

    private fun tryBindImprimanteIntegree() {
        val ctx = getApplication<Application>()
        val intent = Intent().apply {
            setPackage(SUNMI_SERVICE_PACKAGE)
            action = SUNMI_SERVICE_ACTION
        }
        // Absent sur un appareil sans imprimante intégrée (téléphone/tablette classique) :
        // resolveService renvoie null, on reste alors sur le flux Bluetooth externe existant.
        val resolved = try {
            ctx.packageManager.resolveService(intent, 0)
        } catch (e: Exception) {
            null
        }
        if (resolved != null) {
            try {
                ctx.bindService(intent, sunmiServiceConnection, Context.BIND_AUTO_CREATE)
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "Échec liaison imprimante intégrée", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (sunmiService != null) {
            try {
                getApplication<Application>().unbindService(sunmiServiceConnection)
            } catch (_: Exception) {
            }
        }
    }

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
        val sunmi = sunmiService
        if (sunmi != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    printProformaViaSunmi(sunmi, tableItems, total, infos, invoiceId)
                } catch (e: Exception) {
                    Log.e("BluetoothViewModel", "Échec impression imprimante intégrée", e)
                }
            }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1) Reset + taille normale
                writeCmd(0x1B, 0x40)        // ESC @  (initialize)
                writeCmd(0x1D, 0x21, 0x00)  // GS ! 0 (taille normale - largeur/hauteur x1)
                writeCmd(0x1B, 0x45, 0x01)  // ESC E 1 (gras)

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
                sb.append(sepLine())
                sb.append("NOTE PROVISOIRE\r\n")
                sb.append("(Ceci n'est pas un ticket)\r\n")
                sb.append(sepLine())
                if (invoiceId != null){
                    sb.append("FACTURE CLIENT N°: $invoiceNo\r\n")
                    sb.append(sepLine())
                }else {
                    sb.append("FACTURE CLIENT \r\n")
                    sb.append(sepLine())
                }
                sb.append("\u001B\u0061\u0000") // Alignement à gauche

                sb.append(
                    formatLine58(
                        article = "Article",
                        qty = "Qte",
                        price = "Prix",
                        total = "Total"
                    )
                )
                sb.append(sepLine())

                // Colonnes 58mm -> on serre un peu
                tableItems.forEach { ticket ->

                    val article = ticket.produit.nom.replace("\n", " ")
                    val qty = ticket.quantity.toString()
                    val price = ticket.produit.prix.toInt().toString()
                    val totalLine = (ticket.produit.prix * ticket.quantity).toInt().toString().replace(" ", "")

                    sb.append(
                        formatLine58(
                            article = article,
                            qty = qty,
                            price = price,
                            total = totalLine
                        )
                    )
                }


                sb.append(sepLine())
                sb.append("TOTAL: ${formatPrice(total, infos?.devise)}\r\n")
                sb.append(sepLine())
                sb.append("Merci pour votre confiance\r\n")
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
        val sunmi = sunmiService
        if (sunmi != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    printInvoiceViaSunmi(sunmi, tableItems, total, infos, invoiceId, sequenceNumber, signatureHash)
                    onSuccess?.invoke()
                } catch (e: Exception) {
                    Log.e("BluetoothViewModel", "Échec impression imprimante intégrée", e)
                    onError?.invoke(e)
                }
            }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (outputStream == null) {
                    throw IllegalStateException("OutputStream Bluetooth nul")
                }

                writeCmd(0x1B, 0x40)
                writeCmd(0x1D, 0x21, 0x00) // Taille normale
                writeCmd(0x1B, 0x45, 0x01) // ESC E 1 (gras)
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
                sb.append(sepLine())
                sb.append("TICKET N°: $invoiceNo\r\n")
                sb.append(sepLine())
                sb.append("\u001B\u0061\u0000") // Alignement à gauche

                sb.append(
                    formatLine58(
                        article = "Article",
                        qty = "Qte",
                        price = "Prix",
                        total = "Total"
                    )
                )
                sb.append(sepLine())

                tableItems.forEach { ticket ->
                    val article = ticket.produit.nom.replace("\n", " ")
                    val qty = ticket.quantity.toString()
                    val price = formatPriceShort(ticket.produit.prix)
                    val totalLine = formatPriceShort(ticket.produit.prix * ticket.quantity)

                    sb.append(
                        formatLine58(
                            article = article,
                            qty = qty,
                            price = price,
                            total = totalLine
                        )
                    )
                }

                sb.append(sepLine())
                sb.append("TOTAL TTC: ${formatPrice(total, infos?.devise)}\r\n")
                sb.append("TVA (5%): ${formatPrice(montantTVA, infos?.devise)}\r\n")
                sb.append("TOTAL HT : ${formatPrice(montantHT, infos?.devise)}\r\n")

                if (signatureHash != null) {
                    val displayHash = if (signatureHash.length > 8) signatureHash.takeLast(8) else signatureHash
                    sb.append("Signature: $displayHash\r\n")
                }

                sb.append(sepLine())
                sb.append("${AppConfig.NOM_LOGICIEL} v${AppConfig.VERSION_LOGICIEL}\r\n")
                sb.append("${AppConfig.NUM_CERTIFICAT}\r\n")
                sb.append(sepLine())
                sb.append("Merci de votre visite !\r\n")
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
    // ---- Impression via l'imprimante intégrée (Sunmi & compatibles) ----
    // printColumnsString ne produit rien de visible sur ce firmware (le total post-boucle
    // s'imprime, pas les lignes d'articles) — on repasse donc par printText avec un padding
    // manuel, comme pour le Bluetooth classique (formatLine58), qui lui est confirmé fonctionnel
    // via les lignes d'en-tête/total déjà imprimées.
    private val SUNMI_AW = 14
    private val SUNMI_QW = 4
    private val SUNMI_PW = 6
    private val SUNMI_TW = 8

    private fun sunmiFormatRow(article: String, qty: String, price: String, total: String): String {
        fun cut(s: String, w: Int) = if (s.length <= w) s else s.take(w)
        val a = cut(article, SUNMI_AW).padEnd(SUNMI_AW, ' ')
        val q = cut(qty, SUNMI_QW).padStart(SUNMI_QW, ' ')
        val p = cut(price, SUNMI_PW).padStart(SUNMI_PW, ' ')
        val t = cut(total, SUNMI_TW).padStart(SUNMI_TW, ' ')
        return "$a $q $p $t\n"
    }

    private fun sunmiPrintRow(svc: IWoyouService, article: String, qty: String, price: String, total: String) {
        svc.printText(sunmiFormatRow(article, qty, price, total), null)
    }

    // IWoyouService n'a pas de méthode dédiée au gras : on passe par la commande ESC/POS
    // brute ESC E n via sendRAWData, supportée par l'imprimante intégrée comme les externes.
    private fun sunmiSetBold(svc: IWoyouService, bold: Boolean) {
        svc.sendRAWData(byteArrayOf(0x1B, 0x45, if (bold) 0x01 else 0x00), null)
    }

    private fun printProformaViaSunmi(
        svc: IWoyouService,
        tableItems: List<Ticket>,
        total: Double,
        infos: ShopInfos?,
        invoiceId: UUID?
    ) {
        val dateHeure = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date())
        val invoiceNo = invoiceId?.let { invoiceNoFromId(it) }

        svc.printerInit(null)
        // Police plus petite pour que les lignes de 32-35 caractères tiennent sur la largeur
        // réelle du papier sans retour à la ligne (ce qui cassait l'alignement des colonnes).
        svc.setFontSize(20f, null)
        sunmiSetBold(svc, true)
        svc.setAlignment(1, null)
        svc.printText("*** ${infos?.name ?: ""} ***\n", null)
        svc.printText("Adresse: ${infos?.address ?: ""}\n", null)
        svc.printText("Tel: ${infos?.phone ?: ""}\n", null)
        svc.printText("Date: $dateHeure\n", null)
        svc.printText(sepLineSunmi(), null)
        svc.printText("NOTE PROVISOIRE\n", null)
        svc.printText("(Ceci n'est pas un ticket)\n", null)
        svc.printText(sepLineSunmi(), null)
        if (invoiceId != null) {
            svc.printText("FACTURE CLIENT N°: $invoiceNo\n", null)
        } else {
            svc.printText("FACTURE CLIENT\n", null)
        }
        svc.printText(sepLineSunmi(), null)
        svc.setAlignment(0, null)

        sunmiPrintRow(svc, "Article", "Qte", "Prix", "Total")
        svc.printText(sepLineSunmi(), null)
        tableItems.forEach { ticket ->
            val article = ticket.produit.nom.replace("\n", " ")
            val qty = ticket.quantity.toString()
            val price = ticket.produit.prix.toInt().toString()
            val totalLine = (ticket.produit.prix * ticket.quantity).toInt().toString()
            sunmiPrintRow(svc, article, qty, price, totalLine)
        }

        svc.printText(sepLineSunmi(), null)
        svc.printText("TOTAL: ${formatPrice(total, infos?.devise)}\n", null)
        svc.printText(sepLineSunmi(), null)
        svc.setAlignment(1, null)
        svc.printText("Merci pour votre confiance\n", null)
        sunmiSetBold(svc, false)
        svc.lineWrap(3, null)
    }

    private fun printInvoiceViaSunmi(
        svc: IWoyouService,
        tableItems: List<Ticket>,
        total: Double,
        infos: ShopInfos?,
        invoiceId: UUID,
        sequenceNumber: Long,
        signatureHash: String?
    ) {
        val dateHeure = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRANCE).format(Date())
        val invoiceNo = if (sequenceNumber > 0) formatTicketNumber(sequenceNumber)
                        else invoiceNoFromId(invoiceId)
        val montantTVA = total * 5.0 / 120.0
        val montantHT = total - montantTVA

        svc.printerInit(null)
        // Police plus petite pour que les lignes de 32-35 caractères tiennent sur la largeur
        // réelle du papier sans retour à la ligne (ce qui cassait l'alignement des colonnes).
        svc.setFontSize(20f, null)
        sunmiSetBold(svc, true)
        svc.setAlignment(1, null)
        svc.printText("*** ${infos?.name ?: ""} ***\n", null)
        svc.printText("Adresse: ${infos?.address ?: ""}\n", null)
        svc.printText("SIRET: ${infos?.siret ?: "000 000 000"}\n", null)
        svc.printText("Tel: ${infos?.phone ?: ""}\n", null)
        svc.printText("Date: $dateHeure\n", null)
        svc.printText(sepLineSunmi(), null)
        svc.printText("TICKET N°: $invoiceNo\n", null)
        svc.printText(sepLineSunmi(), null)
        svc.setAlignment(0, null)

        sunmiPrintRow(svc, "Article", "Qte", "Prix", "Total")
        svc.printText(sepLineSunmi(), null)
        tableItems.forEach { ticket ->
            val article = ticket.produit.nom.replace("\n", " ")
            val qty = ticket.quantity.toString()
            val price = formatPriceShort(ticket.produit.prix)
            val totalLine = formatPriceShort(ticket.produit.prix * ticket.quantity)
            sunmiPrintRow(svc, article, qty, price, totalLine)
        }

        svc.printText(sepLineSunmi(), null)
        svc.printText("TOTAL TTC: ${formatPrice(total, infos?.devise)}\n", null)
        svc.printText("TVA (5%): ${formatPrice(montantTVA, infos?.devise)}\n", null)
        svc.printText("TOTAL HT : ${formatPrice(montantHT, infos?.devise)}\n", null)
        if (signatureHash != null) {
            val displayHash = if (signatureHash.length > 8) signatureHash.takeLast(8) else signatureHash
            svc.printText("Signature: $displayHash\n", null)
        }
        svc.printText(sepLineSunmi(), null)
        svc.printText("${AppConfig.NOM_LOGICIEL} v${AppConfig.VERSION_LOGICIEL}\n", null)
        svc.printText("${AppConfig.NUM_CERTIFICAT}\n", null)
        svc.printText(sepLineSunmi(), null)
        svc.setAlignment(1, null)
        svc.printText("Merci de votre visite !\n", null)
        sunmiSetBold(svc, false)
        svc.lineWrap(3, null)
    }

    private fun sepLineSunmi(): String = "-".repeat(32) + "\n"

    // Fonction utilitaire pour gagner de la place sur 58mm
    private fun formatPriceShort(amount: Double): String {
        return String.format(Locale.FRANCE, "%.2f", amount)
    }


    // Imprimante 58mm chargée avec du papier 55mm : la zone imprimable réelle est plus étroite
    // que les 42 caractères théoriques de la police B sur un rouleau 58mm plein — au-delà, le
    // texte déborde/tronque en bord de papier et désaligne les colonnes. 38 caractères laisse
    // une marge de sécurité de chaque côté.
    private val LINE_CHARS_58 = 38
    private fun sepLine(): String = "-".repeat(LINE_CHARS_58) + "\r\n"
    private fun formatLine58(article: String, qty: String, price: String, total: String): String {
        val aW = 17
        val qW = 3
        val pW = 7
        val tW = 8

        fun cut(s: String, w: Int) = if (s.length <= w) s else s.take(w)

        val a = cut(article, aW).padEnd(aW, ' ')
        val q = cut(qty, qW).padStart(qW, ' ')
        val p = cut(price, pW).padStart(pW, ' ')
        val t = cut(total, tW).padStart(tW, ' ')

        return "$a $q $p $t\r\n" // 17+1+3+1+7+1+8 = 38
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