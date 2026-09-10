package com.example.caisse.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.caisse.data.DashboardViewModel
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.VendeurSuiviUi
import com.example.caisse.model.AuthViewModel
import com.example.caisse.util.formatPrice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuiviVendeursScreen(navController: NavController, viewModel: DashboardViewModel, authViewModel: AuthViewModel, menuViewModel: MenuViewModel) {
    val suivi by viewModel.suiviVendeurs.collectAsState()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val shopInfos = menuViewModel.getInfos()
    // Les heures d'ouverture/fermeture viennent d'autres appareils : on force une synchro à
    // l'ouverture de cet écran plutôt que d'attendre le prochain cycle périodique.
    LaunchedEffect(Unit) {
        authViewModel.enqueueSyncUnique(context = ctx)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suivi des vendeurs") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            items(suivi) { vendeurSuivi ->
                SuiviVendeurCard(vendeurSuivi, shopInfos?.devise)
            }
        }
    }
}

@Composable
private fun SuiviVendeurCard(suivi: VendeurSuiviUi, devise: String? = null) {
    val sdf = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(suivi.nom, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                    color = if (suivi.ouverte) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (suivi.ouverte) "Ouverte" else "Fermée",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Ouverture : ${suivi.heureOuverture?.let { sdf.format(Date(it)) } ?: "—"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Fermeture : ${suivi.heureFermeture?.let { sdf.format(Date(it)) } ?: if (suivi.ouverte) "en cours" else "—"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Ventes de la session : ${formatPrice(suivi.totalVentesSession, devise = devise)} ",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
