package com.example.oudeika.util

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.oudeika.data.MenuViewModel

@Composable
fun ChangePasswordDialog(
    viewModel: MenuViewModel,
    onDismiss: () -> Unit
) {
    var oldPwd by remember { mutableStateOf("") }
    var newPwd by remember { mutableStateOf("") }
    var confirmPwd by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val canSubmit = oldPwd.isNotBlank() && newPwd.isNotBlank() && confirmPwd.isNotBlank()

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text("Changer le mot de passe") },
        text = {
            Column {
                OutlinedTextField(
                    value = oldPwd,
                    onValueChange = { oldPwd = it; error = null },
                    label = { Text("Ancien mot de passe") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = newPwd,
                    onValueChange = { newPwd = it; error = null },
                    label = { Text("Nouveau mot de passe") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = confirmPwd,
                    onValueChange = { confirmPwd = it; error = null },
                    label = { Text("Confirmer") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (!error.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSubmit && !isSubmitting,
                onClick = {
                    if (newPwd != confirmPwd) {
                        error = "Les mots de passe ne correspondent pas"
                        return@TextButton
                    }
                    if (newPwd.length < 6) {
                        error = "Minimum 6 caractères"
                        return@TextButton
                    }

                    isSubmitting = true
                    val result = viewModel.changePassword(oldPwd, newPwd)
                    isSubmitting = false

                    if (result.isSuccess) onDismiss()
                    else error = result.exceptionOrNull()?.message ?: "Erreur"
                }
            ) { Text("Valider") }
        },
        dismissButton = {
            TextButton(enabled = !isSubmitting, onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
