package com.example.caisse

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.caisse.composable.AppNavigation
import com.example.caisse.kiosk.KioskManager
import com.example.caisse.ui.theme.CaisseTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Applique les politiques Device Owner (whitelist LockTask, launcher HOME, restrictions).
        // Sans effet si l'app n'est pas encore Device Owner.
        KioskManager.applyKioskPolicies(this)

        setContent {
            AppNavigation()
        }
    }

    override fun onResume() {
        super.onResume()
        // (Ré)active le kiosk à chaque retour au premier plan, sauf si l'admin a demandé une sortie.
        if (KioskManager.isDeviceOwner(this) && !KioskManager.adminMaintenanceMode) {
            KioskManager.startKiosk(this)
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CaisseTheme {
        Greeting("Android")
    }
}