package com.example.caisse.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.caisse.data.MenuViewModel
import com.example.caisse.data.ShopInfos
import com.example.caisse.ui.theme.MintEnd
import com.example.caisse.ui.theme.MintStart
import com.example.caisse.ui.theme.Slate100
import com.example.caisse.ui.theme.Slate500
import com.example.caisse.ui.theme.Slate700
import com.example.caisse.ui.theme.Slate900
import com.example.caisse.util.ChangePasswordDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfosScreenStart(
    navController: NavController,
    viewModel: MenuViewModel
) {
    // Charger l’existant (si présent) puis remplir les champs
    var existing by remember { mutableStateOf<ShopInfos?>(null) }
    var isEdit by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var devise by remember { mutableStateOf("") }
    var siret by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val listDevise = listOf("FCFA", "€", "£", "US$")
    var showChangePassword by remember { mutableStateOf(false) }
    // Mot de passe gestion défini à la CRÉATION de la fiche (plus de "1234" par défaut)
    var initialPwd by remember { mutableStateOf("") }
    var initialPwdConfirm by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Sur un nouvel appareil, la fiche peut arriver du cloud quelques secondes après
        // l'ouverture de l'écran : on déclenche une sync et on OBSERVE la base (au lieu d'une
        // lecture unique) pour afficher la fiche dès son import — et éviter que l'utilisateur
        // en recrée une par-dessus celle du cloud.
        androidx.work.WorkManager.getInstance(navController.context).enqueueUniqueWork(
            "sync_unique",
            androidx.work.ExistingWorkPolicy.KEEP,
            androidx.work.OneTimeWorkRequestBuilder<com.example.caisse.model.SyncWorker>()
                .addTag("sync").build()
        )
        viewModel.observeInfos().collect { info ->
            val hadNone = existing == null
            existing = info
            if (info != null) {
                // Remplir les champs à la 1re émission ou tant qu'on n'est pas en édition
                if (hadNone || !isEdit) {
                    isEdit = false
                    name = info.name
                    address = info.address
                    phone = info.phone
                    email = info.email
                    devise = info.devise
                    siret = info.siret
                }
            } else {
                // Pas d’infos -> mode édition
                isEdit = true
            }
        }
    }

    val pwdOk = existing != null || (initialPwd.length >= 4 && initialPwd == initialPwdConfirm)
    val canSave = name.isNotBlank() && email.isNotBlank() && pwdOk

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Informations du magasin", style = MaterialTheme.typography.titleLarge, color = Slate900)
                        Text(
                            if (isEdit) "Renseignez ou modifiez vos coordonnées" else "Vos coordonnées",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (existing != null) {
                        // Si une fiche existe : bouton pour basculer Edition / Lecture
                      //  IconButton(onClick = { isEdit = !isEdit }) {
                        //    Icon(Icons.Filled.Edit, contentDescription = "Modifier")
                       // }
                    }
                    if (isEdit && canSave) {
                        IconButton(onClick = {
                            scope.launch {
                                if (existing == null) {
                                    viewModel.addInfos(
                                        name = name.trim(),
                                        address = address.trim(),
                                        phone = phone.trim(),
                                        email = email.trim(),
                                        siret = siret,
                                        logo = null,
                                        devise = devise,
                                        initialPassword = initialPwd.trim()
                                    )
                                } else {
                                    viewModel.updateInfos(
                                        name = name.trim(),
                                        address = address.trim(),
                                        phone = phone.trim(),
                                        email = email.trim(),
                                        siret = siret,
                                        logo = null,
                                        devise = devise
                                    )
                                }
                                navController.popBackStack()
                            }
                        }) {
                            Icon(Icons.Filled.Save, contentDescription = "Enregistrer")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Slate100
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Card(
                elevation = cardElevation(6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(Modifier.padding(14.dp)) {

                    Text("Coordonnées", style = MaterialTheme.typography.titleMedium, color = Slate900)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .fillMaxWidth(0.45f)
                            .background(
                                Brush.linearGradient(listOf(MintStart, MintEnd)),
                                shape = MaterialTheme.shapes.medium
                            )
                    )
                    Spacer(Modifier.height(16.dp))

                    if (isEdit) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(1.dp)
                                .verticalScroll(scrollState),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            // ------- Mode édition : formulaire -------
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Nom de la boutique") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = siret,
                                onValueChange = { siret = it },
                                label = { Text("SIRET") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))

                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Adresse") },
                                singleLine = false,
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))

                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Téléphone") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))

                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = devise,
                                    onValueChange = {},            // lecture seule
                                    readOnly = true,
                                    label = { Text("Devise") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                    },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )

                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    listDevise.forEach { currency ->
                                        DropdownMenuItem(
                                            text = { Text(currency) },
                                            onClick = {
                                                devise = currency
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            if (existing == null) {
                                Spacer(Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = initialPwd,
                                    onValueChange = { initialPwd = it },
                                    label = { Text("Mot de passe gestion (min. 4 caractères)") },
                                    singleLine = true,
                                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = initialPwdConfirm,
                                    onValueChange = { initialPwdConfirm = it },
                                    label = { Text("Confirmer le mot de passe") },
                                    singleLine = true,
                                    isError = initialPwdConfirm.isNotEmpty() && initialPwd != initialPwdConfirm,
                                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    enabled = canSave,
                                    onClick = {
                                        scope.launch {
                                            if (existing == null) {
                                                viewModel.addInfos(
                                                    name = name.trim(),
                                                    address = address.trim(),
                                                    phone = phone.trim(),
                                                    email = email.trim(),
                                                    logo = null,
                                                    siret = siret.trim(),
                                                    devise = devise,
                                                    initialPassword = initialPwd.trim()
                                                )
                                            } else {
                                                viewModel.updateInfos(
                                                    name = name.trim(),
                                                    address = address.trim(),
                                                    phone = phone.trim(),
                                                    email = email.trim(),
                                                    logo = null,
                                                    siret = siret.trim(),
                                                    devise = devise
                                                )
                                            }
                                            navController.popBackStack()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.Save, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Enregistrer", fontWeight = FontWeight.SemiBold)
                                }
                            }
                            if (showChangePassword) {
                                ChangePasswordDialog(
                                    viewModel = viewModel,
                                    onDismiss = { showChangePassword = false }
                                )
                            }
                            if (!canSave) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = if (existing == null)
                                        "Le nom, l’email et le mot de passe gestion (min. 4 caractères, confirmé) sont obligatoires."
                                    else
                                        "Le nom et l’email sont obligatoires.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate700
                                )
                            }
                        }

                    } else {
                        // ------- Mode lecture : afficher la fiche -------
                        if (existing != null) {
                            ReadOnlyRow("Nom", existing!!.name)
                            ReadOnlyRow("Adresse", existing!!.address)
                            ReadOnlyRow("Téléphone", existing!!.phone)
                            ReadOnlyRow("Email", existing!!.email)
                            ReadOnlyRow("devise", existing!!.devise)

                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(onClick = { isEdit = true }) {
                                    Icon(Icons.Filled.Edit, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Modifier")
                                }
                            }
                        } else {
                            // Sécurité (rare) : si pas d’infos mais isEdit == false
                            Text(
                                "Aucune information enregistrée.",
                                color = Slate700,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { isEdit = true }) {
                                Text("Ajouter des informations")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = Slate700)
        Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}
