package com.example.gestion.composable



import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gestion.data.Category
import com.example.gestion.data.MenuViewModel

/**
 * Écran simple pour gérer une liste de catégories : ajouter, renommer, supprimer.
 * L'état est conservé à la rotation via rememberSaveable + Saver custom.
 * Vous pourrez brancher une persistance (Room/DataStore) plus tard facilement.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    viewModelcategories: MenuViewModel

) {
    val categories by viewModelcategories.categories.collectAsState()
    var newName by remember { mutableStateOf(TextFieldValue("")) }
    var selectedTab by remember { mutableIntStateOf(0) }
    // État du dialogue de renommage
    var renameTarget by remember { mutableStateOf<Category?>(null) }
    var renameText by remember { mutableStateOf(TextFieldValue("")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catégories", style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                    }
                }

            )
        },
        bottomBar = {
            BottomHome(
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it },
                navController = navController
            )
        }

    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            Spacer(Modifier.height(12.dp))

            // Ligne d'ajout
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nouvelle catégorie") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val trimmed = newName.text.trim()
                    if (trimmed.isNotEmpty() && categories.none { it.name.equals(trimmed, ignoreCase = true) }) {
                        viewModelcategories.addCategory(trimmed)
                        newName = TextFieldValue("")
                    }
                }) {
                    Text("Ajouter")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Liste
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(categories, key = { it.id }) { cat ->
                    CategoryRow(
                        category = cat,
                        onRename = {
                            renameTarget = cat
                            renameText = TextFieldValue(cat.name)
                        },
                        onDelete = {
                            viewModelcategories.deleteCategory(cat)
                        }
                    )
                }
            }
        }

        if (renameTarget != null) {
            AlertDialog(
                onDismissRequest = { renameTarget = null },
                title = { Text("Renommer la catégorie") },
                text = {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        singleLine = true,
                        label = { Text("Nom") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val trimmed = renameText.text.trim()
                        val target = renameTarget
                        if (target != null && trimmed.isNotEmpty() &&
                            categories.none { it.name.equals(trimmed, ignoreCase = true) && it.id != target.id }
                        ) {
                            viewModelcategories.renameCategory(target.id, trimmed)
                            renameTarget = null
                        }
                    }) { Text("Enregistrer") }
                },
                dismissButton = {
                    TextButton(onClick = { renameTarget = null }) { Text("Annuler") }
                }
            )
        }
    }

}

@Composable
private fun CategoryRow(
    category: Category,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(category.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = onRename) {
                Icon(Icons.Default.Edit, contentDescription = "Renommer")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Supprimer")
            }
        }
    }
}





@Preview
@Composable
fun CategoriesScreenPreview() {
   // CategoriesScreen( modifier = Modifier, navController = NavController(LocalContext.current), categories = CategorieViewmodel())
}