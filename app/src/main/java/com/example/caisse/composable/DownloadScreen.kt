package com.example.caisse.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.caisse.data.MenuViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownLoadScreen(menuViewModel: MenuViewModel, navController: NavController) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export des données (NF525)") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Conformément à l'Axe D de la NF525, vous pouvez exporter vos données de vente et votre journal technique.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            // Bouton pour la base de données (Ventes)
            Button(
                onClick = { menuViewModel.exportVentesToCSV(context) },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Dataset, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Télécharger la base des Ventes (CSV)")
            }

            // Bouton pour le JET (Journal des Événements Techniques)
            Button(
                onClick = { menuViewModel.exportJETToCSV(context) },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.HistoryEdu, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Télécharger le JET (CSV)")
            }
        }
    }
}