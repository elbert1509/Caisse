package com.example.caisse.kiosk

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context

/**
 * Récepteur d'administration de l'appareil.
 * Nécessaire pour que l'app puisse devenir "Device Owner" et activer le mode kiosk
 * (LockTask verrouillé) que l'utilisateur ne peut pas quitter.
 */
class KioskAdminReceiver : DeviceAdminReceiver() {

    companion object {
        /** Composant ADMIN utilisé partout pour piloter le DevicePolicyManager. */
        fun getComponentName(context: Context): ComponentName =
            ComponentName(context.applicationContext, KioskAdminReceiver::class.java)
    }
}
