package com.example.caisse.composable

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.caisse.bluetooth.BluetoothViewModel
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.Ticket
import com.example.caisse.data.AppConfig
import com.example.caisse.data.TypeEvenement
import com.example.caisse.util.formatPrice
import com.example.caisse.util.formatTicketNumber
import com.example.caisse.util.invoiceNoFromId
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// ─── Couleurs ticket ─────────────────────────────────────────────────────────
private val TicketBg        = Color(0xFFF7F5F0)   // papier crème
private val TicketSurface   = Color(0xFFFFFFFF)
private val TicketPrimary   = Color(0xFF1A1A2E)   // encre foncée
private val TicketAccent    = Color(0xFF2563EB)   // bleu pro
private val TicketMuted     = Color(0xFF6B7280)
private val TicketDivider   = Color(0xFFD1D5DB)
private val TicketSuccess   = Color(0xFF16A34A)

// ─── Composable principal ─────────────────────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(
    venteId: String,
    navController: NavController,
    viewModel: MenuViewModel,
    bluetoothViewModel: BluetoothViewModel
) {
    val context = LocalContext.current
    val infos   = viewModel.getInfos()
    val devise  = infos?.devise ?: ""

    val venteUuid    = remember(venteId) { UUID.fromString(venteId) }
    val venteDetails by viewModel.selectedVente.collectAsState()
    val isPrinting   by bluetoothViewModel.isPrinting.collectAsState()
    val isConnected  by bluetoothViewModel.isConnected.collectAsState()

    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(venteUuid) {
        viewModel.loadVenteWithDetailsById(venteUuid)
    }
    LaunchedEffect(venteDetails) {
        if (venteDetails != null) visible = true
    }

    Scaffold(
        containerColor = TicketBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Ticket de caisse",
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 17.sp,
                        color      = TicketPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour", tint = TicketPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TicketBg),
                actions = {
                    // Bouton Imprimer avec état loading
                    IconButton(
                        onClick = {
                            if (!isConnected) {
                                Toast.makeText(context, "Aucune imprimante connectée", Toast.LENGTH_SHORT).show()
                                viewModel.loggerEvenement("Impression Annulée", "Pas de device connecté")
                                return@IconButton
                            }
                            val vente = venteDetails ?: return@IconButton
                            val tickets = vente.lignes.map { Ticket(it.produit, it.ligne.quantity) }
                            bluetoothViewModel.printInvoice(
                                tableItems     = tickets,
                                total          = vente.vente.total,
                                infos          = infos,
                                invoiceId      = vente.vente.id,
                                sequenceNumber = vente.vente.sequenceNumber,
                                signatureHash  = vente.vente.hash,
                                onSuccess = {
                                    viewModel.loggerEvenement(
                                        type        = TypeEvenement.IMPRESSION_TICKET.name,
                                        description = "Ticket seq=${vente.vente.sequenceNumber} imprimé"
                                    )
                                },
                                onError = { e ->
                                    Toast.makeText(context, "Erreur d'impression", Toast.LENGTH_SHORT).show()
                                    viewModel.loggerEvenement(
                                        type        = TypeEvenement.IMPRESSION_ANNULEE.name,
                                        description = "Erreur: ${e.message}"
                                    )
                                }
                            )
                          //  bluetoothViewModel.testPrint(viewModel)

                        },
                        enabled = !isPrinting
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(
                                modifier  = Modifier.size(20.dp),
                                color     = TicketAccent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector        = Icons.Default.Print,
                                contentDescription = "Imprimer",
                                tint               = if (isConnected) TicketAccent else TicketMuted
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (venteDetails == null) {
            // ── État chargement ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = TicketAccent)
                    Spacer(Modifier.height(12.dp))
                    Text("Chargement du ticket…", color = TicketMuted, fontSize = 14.sp)
                }
            }
            return@Scaffold
        }

        val vente   = venteDetails ?: return@Scaffold
        val tickets = vente.lignes.map { Ticket(it.produit, it.ligne.quantity) }

        val formattedDate = remember(vente.vente.date) {
            SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.FRANCE).format(Date(vente.vente.date))
        }
        val invoiceNo = remember(vente.vente.sequenceNumber, vente.vente.id) {
            if (vente.vente.sequenceNumber > 0) formatTicketNumber(vente.vente.sequenceNumber)
            else invoiceNoFromId(vente.vente.id)
        }

        val montantTTC = vente.vente.total
        // NF525 : TVA calculée à partir des taux réels de chaque ligne
        val montantTVA = vente.lignes.sumOf { l ->
            l.ligne.sousTotal * l.ligne.tauxTVA / (100.0 + l.ligne.tauxTVA)
        }
        val montantHT  = montantTTC - montantTVA

        // ── Ticket principal ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter   = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Corps du ticket ──────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TicketSurface)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {

                        // En-tête coloré
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(TicketPrimary)
                                .padding(vertical = 20.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text       = infos?.name?.uppercase() ?: "MON MAGASIN",
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = Color.White,
                                letterSpacing = 2.sp,
                                textAlign  = TextAlign.Center
                            )
                            if (!infos?.address.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text      = infos!!.address,
                                    fontSize  = 11.sp,
                                    color     = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (!infos?.phone.isNullOrBlank()) {
                                Text(
                                    text     = "Tél : ${infos!!.phone}",
                                    fontSize = 11.sp,
                                    color    = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                            Spacer(Modifier.height(16.dp))

                            // Numéro et date
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.Top
                            ) {
                                Column {
                                    Text("TICKET N°", fontSize = 9.sp, color = TicketMuted, letterSpacing = 1.sp)
                                    Text(
                                        text       = invoiceNo,
                                        fontSize   = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color      = TicketPrimary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("DATE", fontSize = 9.sp, color = TicketMuted, letterSpacing = 1.sp)
                                    Text(
                                        text     = formattedDate,
                                        fontSize = 12.sp,
                                        color    = TicketPrimary
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            TicketDashedDivider()
                            Spacer(Modifier.height(12.dp))

                            // En-tête colonnes
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("ARTICLE",  Modifier.weight(1f), fontSize = 9.sp, color = TicketMuted, letterSpacing = 1.sp)
                                Text("QTÉ",      Modifier.width(32.dp), fontSize = 9.sp, color = TicketMuted, letterSpacing = 1.sp, textAlign = TextAlign.Center)
                                Text("P.U",      Modifier.width(60.dp), fontSize = 9.sp, color = TicketMuted, letterSpacing = 1.sp, textAlign = TextAlign.End)
                                Text("TOTAL",    Modifier.width(68.dp), fontSize = 9.sp, color = TicketMuted, letterSpacing = 1.sp, textAlign = TextAlign.End)
                            }

                            Spacer(Modifier.height(8.dp))

                            // Lignes articles
                            tickets.forEach { ticket ->
                                TicketLineItem(ticket = ticket, devise = devise)
                                Spacer(Modifier.height(6.dp))
                            }

                            Spacer(Modifier.height(8.dp))
                            TicketDashedDivider()
                            Spacer(Modifier.height(12.dp))

                            // Sous-totaux
                            TicketSummaryRow("Total HT",      formatPrice(montantHT,  devise), muted = true)
                            Spacer(Modifier.height(4.dp))
                            TicketSummaryRow("TVA (20%)",     formatPrice(montantTVA, devise), muted = true)
                            Spacer(Modifier.height(10.dp))

                            // Total TTC en évidence
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TicketPrimary)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Text("TOTAL TTC", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
                                Text(
                                    text       = formatPrice(montantTTC, devise),
                                    fontSize   = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color      = Color.White
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            // SIRET si dispo
                            if (!infos?.siret.isNullOrBlank()) {
                                Text(
                                    text      = "SIRET : ${infos!!.siret}",
                                    fontSize  = 10.sp,
                                    color     = TicketMuted,
                                    modifier  = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(6.dp))
                            }

                            // Signature / hash
                            if (!vente.vente.hash.isNullOrBlank()) {
                                TicketDashedDivider()
                                Spacer(Modifier.height(8.dp))
                                Text("SIGNATURE ÉLECTRONIQUE", fontSize = 9.sp, color = TicketMuted, letterSpacing = 1.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text       = vente.vente.hash.takeLast(16),
                                    fontSize   = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color      = TicketAccent,
                                    modifier   = Modifier.fillMaxWidth(),
                                    textAlign  = TextAlign.Center,
                                    letterSpacing = 1.sp
                                )
                                Spacer(Modifier.height(8.dp))
                            }

                            TicketDashedDivider()
                            Spacer(Modifier.height(12.dp))

                            // Pied de ticket
                            Text(
                                text      = "Merci de votre visite !",
                                fontSize  = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color     = TicketPrimary,
                                modifier  = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text      = "${AppConfig.NOM_LOGICIEL} v${AppConfig.VERSION_LOGICIEL} • ${AppConfig.NUM_CERTIFICAT}",
                                fontSize  = 9.sp,
                                color     = TicketMuted,
                                modifier  = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(20.dp))
                        }
                    }
                }

                // Effet déchirure bas du ticket
                TicketTearEdge()

                Spacer(Modifier.height(24.dp))

                // Badge statut impression
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isConnected) TicketSuccess else TicketDivider)
                    )
                    Text(
                        text     = if (isConnected) "Imprimante connectée" else "Aucune imprimante",
                        fontSize = 12.sp,
                        color    = if (isConnected) TicketSuccess else TicketMuted
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ─── Ligne article ────────────────────────────────────────────────────────────
@Composable
private fun TicketLineItem(ticket: Ticket, devise: String) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text     = ticket.produit.nom,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color    = TicketPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text      = "×${ticket.quantity}",
            modifier  = Modifier.width(32.dp),
            fontSize  = 12.sp,
            color     = TicketMuted,
            textAlign = TextAlign.Center
        )
        Text(
            text      = formatPrice(ticket.produit.prix, devise),
            modifier  = Modifier.width(60.dp),
            fontSize  = 12.sp,
            color     = TicketMuted,
            textAlign = TextAlign.End
        )
        Text(
            text       = formatPrice(ticket.produit.prix * ticket.quantity, devise),
            modifier   = Modifier.width(68.dp),
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color      = TicketPrimary,
            textAlign  = TextAlign.End
        )
    }
}

// ─── Ligne sous-total ─────────────────────────────────────────────────────────
@Composable
private fun TicketSummaryRow(label: String, value: String, muted: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = if (muted) TicketMuted else TicketPrimary)
        Text(value, fontSize = 12.sp, color = if (muted) TicketMuted else TicketPrimary, fontWeight = FontWeight.Medium)
    }
}

// ─── Séparateur pointillé ─────────────────────────────────────────────────────
@Composable
private fun TicketDashedDivider() {
    val color = TicketDivider
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .drawBehind {
                drawDashedLine(color, size.width)
            }
    )
}

private fun DrawScope.drawDashedLine(color: Color, width: Float) {
    drawLine(
        color       = color,
        start       = Offset(0f, 0f),
        end         = Offset(width, 0f),
        strokeWidth = 2f,
        pathEffect  = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
    )
}

// ─── Effet déchirure bas de ticket ───────────────────────────────────────────
@Composable
private fun TicketTearEdge() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        repeat(20) {
            Box(
                modifier = Modifier
                    .size(width = 8.dp, height = 12.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart    = 0.dp,
                            topEnd      = 0.dp,
                            bottomStart = 8.dp,
                            bottomEnd   = 8.dp
                        )
                    )
                    .background(TicketBg)
            )
        }
    }
}