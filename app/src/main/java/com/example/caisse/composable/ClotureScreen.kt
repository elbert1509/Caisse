package com.example.caisse.composable
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.caisse.data.Cloture
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.TypeEvenement
import com.example.caisse.data.Vente
import com.example.caisse.util.PdfReportGenerator
import com.example.caisse.util.formatPrice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ClotureScreen(viewModel: MenuViewModel, onBack: () -> Unit,) {
    val context = LocalContext.current
    val ventes by viewModel.ventes.collectAsState()
    val infos by viewModel.observeInfos().collectAsState(initial = null)
    val devise = infos?.devise ?: "€"

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var isSharing by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    val ventesDuJour = remember(ventes) {
        ventes.filter { vente ->
            !vente.isDeleted && vente.toLocalDate() == today
        }
    }

    val totalJour = remember(ventesDuJour) {
        ventesDuJour.sumOf { it.total }
    }

    val derniereCloture by produceState<Cloture?>(initialValue = null) {
        value = try {
            viewModel.repository.venteDao.getLastCloture()
        } catch (_: Exception) {
            null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cloture ") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        enabled = !isSharing,
                        onClick = {
                            scope.launch {
                                isSharing = true
                                try {
                                    val startOfDay = today.atStartOfDay(ZoneId.systemDefault())
                                        .toInstant().toEpochMilli()
                                    val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault())
                                        .toInstant().toEpochMilli()
                                    val items = viewModel.repository.venteDao
                                        .getProductReportBetween(startOfDay, endOfDay).first()

                                    val file = withContext(Dispatchers.IO) {
                                        PdfReportGenerator.generateRapportProduits(
                                            destDir = context.cacheDir,
                                            infos = infos,
                                            title = "Clôture du ${today.format(formatter)}",
                                            items = items,
                                            total = totalJour
                                        )
                                    }
                                    val uri = FileProvider.getUriForFile(
                                        context, "${context.packageName}.fileprovider", file
                                    )
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        putExtra(Intent.EXTRA_SUBJECT, "Clôture du ${today.format(formatter)}")
                                    }
                                    context.startActivity(
                                        Intent.createChooser(share, "Partager le détail de clôture")
                                    )
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(
                                        "Échec du partage : ${e.message ?: "inconnue"}"
                                    )
                                } finally {
                                    isSharing = false
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Partager le détail")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = "Clôture de caisse",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "La clôture journalière fige les ventes du jour et produit un état comptable durable.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "État du jour - ${today.format(formatter)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ResumeLine(
                        label = "Nombre de ventes",
                        value = ventesDuJour.size.toString()
                    )

                    ResumeLine(
                        label = "Chiffre d'affaires du jour",
                        value = formatPrice(totalJour, devise)
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Dernière clôture enregistrée",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (derniereCloture == null) {
                        Text(
                            text = "Aucune clôture enregistrée pour le moment.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        ResumeLine("Date", derniereCloture!!.dateCloture)
                        ResumeLine("Type", derniereCloture!!.type)
                        ResumeLine(
                            "CA brut",
                            formatPrice(derniereCloture!!.chiffreAffaireBrut, devise)
                        )
                        ResumeLine(
                            "TVA",
                            formatPrice(derniereCloture!!.totalTVA, devise)
                        )
                        ResumeLine(
                            "Compteur ventes",
                            derniereCloture!!.compteurVentes.toString()
                        )
                        ResumeLine(
                            "Grand total cumulé",
                            formatPrice(derniereCloture!!.grandTotalCumule, devise)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Empreinte : ${derniereCloture!!.hash.ifBlank { "non calculée" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Important",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Une clôture ne doit pas être régénérée plusieurs fois pour la même journée. Elle doit servir à figer les données du jour et à constituer une trace comptable inaltérable.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Button(
                onClick = { showConfirmDialog = true },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text("Clôturer la journée")
                }
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        val refreshed = try {
                            viewModel.repository.venteDao.getLastCloture()
                        } catch (_: Exception) {
                            null
                        }

                        snackbarHostState.showSnackbar(
                            if (refreshed != null) {
                                "Dernière clôture : ${refreshed.dateCloture}"
                            } else {
                                "Aucune clôture trouvée"
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Actualiser")
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isLoading) showConfirmDialog = false
            },
            title = {
                Text("Confirmer la clôture")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Date : ${today.format(formatter)}")
                    Text("Ventes : ${ventesDuJour.size}")
                    Text("Montant : ${formatPrice(totalJour, devise)}")
                    Text(
                        "Cette opération doit figer les données journalières pour créer un état comptable durable."
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                //viewModel.clotureJournaliere(                                )
                                val cloture = viewModel.repository.executerClotureGlobale()
                                snackbarHostState.showSnackbar(
                                    "Clôture enregistrée : ${cloture.dateCloture}"
                                )
                                viewModel.loggerEvenement(
                                    type = TypeEvenement.CLOTURE_ET_FERMETURE.name,
                                    description = "Hash=${cloture.hash} | GT=${cloture.grandTotalCumule} | TVA=${cloture.totalTVA}"
                                )

                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(
                                    "Erreur clôture : ${e.message ?: "inconnue"}"
                                )
                                viewModel.loggerEvenement(
                                    type = TypeEvenement.ERREUR_CLOTURE.name,
                                    description = e.message ?: "Erreur inconnue"
                                )
                            } finally {
                                isLoading = false
                                showConfirmDialog = false
                            }
                        }

                    },
                    enabled = !isLoading
                ) {
                    Text("Valider")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isLoading) showConfirmDialog = false
                    }
                ) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun ResumeLine(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatCurrency(amount: Double, devise: String): String {
    val formatted = NumberFormat.getNumberInstance(Locale.FRANCE).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(amount)

    return "$formatted $devise"
}

@RequiresApi(Build.VERSION_CODES.O)
private fun Vente.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(date)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}