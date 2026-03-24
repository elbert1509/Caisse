package com.example.caisse.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.caisse.data.AppConfig

@Composable
fun InfosLegalesScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Information de Certification", style = MaterialTheme.typography.h6)
        Text("Logiciel : ${AppConfig.NOM_LOGICIEL}")
        Text("Version : ${AppConfig.VERSION_LOGICIEL}")
        Text("Éditeur : ${AppConfig.EDITEUR}")
        Text("Certificat : ${AppConfig.NUM_CERTIFICAT}")
        Spacer(modifier = Modifier.height(20.dp))
        Text("Ce logiciel est conforme aux exigences d'inaltérabilité, de sécurisation, de conservation et d'archivage des données conformément à l'article 286 du code général des impôts.",
            style = MaterialTheme.typography.caption)
    }
}