package com.example.caisse.kiosk

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.app.admin.SystemUpdatePolicy
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager
import android.provider.Settings
import android.util.Log

/**
 * Centralise toute la logique du mode kiosk (Device Owner + LockTask).
 *
 * Pré-requis : l'app doit être Device Owner. Voir la procédure ADB :
 *   adb shell dpm set-device-owner com.example.caisse/.kiosk.KioskAdminReceiver
 * (appareil sans aucun compte Google, app installée au préalable).
 */
object KioskManager {

    private const val TAG = "KioskManager"

    /**
     * Vrai quand l'admin a saisi le PIN et travaille hors kiosk (maintenance WiFi/BT).
     * Empêche onResume() de re-verrouiller automatiquement l'écran.
     * Remettre à false via [endMaintenance] pour réactiver le kiosk.
     */
    @Volatile
    var adminMaintenanceMode: Boolean = false
        private set

    /** PIN admin par défaut — À CHANGER avant déploiement (idéalement stocké chiffré). */
    var adminPin: String = "729361"

    /** Vérifie le PIN admin et bascule en maintenance (sort du kiosk) si correct. */
    fun tryEnterMaintenance(activity: Activity, pin: String): Boolean {
        if (pin != adminPin) return false
        adminMaintenanceMode = true
        stopKiosk(activity)
        return true
    }

    /** Termine la maintenance et réactive le kiosk. */
    fun endMaintenance(activity: Activity) {
        adminMaintenanceMode = false
        startKiosk(activity)
    }

    /** Paquets autorisés à s'exécuter pendant le LockTask (ton app + les Réglages pour la maintenance). */
    private val LOCK_TASK_PACKAGES = arrayOf(
        "com.example.caisse",
        "com.android.settings" // permet d'ouvrir WiFi/Bluetooth pendant la maintenance
    )

    /**
     * Mode « lockdown » par défaut du VPN permanent [BlockingVpnService], utilisé quand le
     * champ Firestore `vpnLockdown` est absent (voir [NetworkBlockController]).
     * - false : si le VPN tombe, les autres applis retrouvent le réseau le temps qu'il redémarre.
     * - true  : le système bloque tout trafic hors VPN, même quand le VPN est arrêté.
     * À valider sur Android 7 : vérifier que la caisse garde bien le réseau avant de passer à true.
     */
    const val VPN_LOCKDOWN = false

    /** Play Store, masqué sur les pads (inutile en kiosk, consommateur de données). */
    private const val PLAY_STORE_PACKAGE = "com.android.vending"

    private fun dpm(context: Context): DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    /** Vrai si l'app est bien Device Owner (prérequis pour un kiosk verrouillé). */
    fun isDeviceOwner(context: Context): Boolean =
        dpm(context).isDeviceOwnerApp(context.packageName)

    /** Vrai si l'activité courante est actuellement en LockTask. */
    fun isLockTaskActive(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
        } else {
            @Suppress("DEPRECATION")
            am.isInLockTaskMode
        }
    }

    /**
     * Configure les politiques Device Owner (à appeler une fois au démarrage si Device Owner).
     * - whitelist LockTask
     * - définit l'app comme launcher HOME persistant
     * - bloque l'ajout d'utilisateurs / le mode sans échec
     */
    fun applyKioskPolicies(activity: Activity) {
        if (!isDeviceOwner(activity)) {
            Log.w(TAG, "Pas Device Owner : politiques kiosk ignorées.")
            return
        }
        val dpm = dpm(activity)
        val admin = KioskAdminReceiver.getComponentName(activity)

        // Paquets autorisés en LockTask
        dpm.setLockTaskPackages(admin, LOCK_TASK_PACKAGES)

        // Empêcher la sortie via certaines restrictions
        dpm.addUserRestriction(admin, "no_safe_boot")
        dpm.addUserRestriction(admin, "no_factory_reset")
        dpm.addUserRestriction(admin, "no_add_user")
        // On NE bloque PAS le WiFi ni le Bluetooth : nécessaires pour l'impression et la maintenance.

        // Rendre l'app launcher HOME persistant (revient dans l'app après un reboot).
        val filter = android.content.IntentFilter(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        val home = android.content.ComponentName(activity, "com.example.caisse.MainActivity")
        dpm.addPersistentPreferredActivity(admin, filter, home)

        applyNetworkPolicies(dpm, admin)
    }

    /**
     * Active ou lève le VPN permanent « trou noir » (API 24+). Piloté à distance par
     * [NetworkBlockController] (champ Firestore `vpnBlocking`). Retourne true si appliqué.
     * Activé, le système démarre BlockingVpnService et le relance au boot.
     */
    fun setNetworkBlocking(context: Context, enabled: Boolean, lockdown: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !isDeviceOwner(context)) return false
        val dpm = dpm(context)
        val admin = KioskAdminReceiver.getComponentName(context)
        return try {
            if (enabled) {
                dpm.setAlwaysOnVpnPackage(admin, context.packageName, lockdown)
            } else {
                dpm.setAlwaysOnVpnPackage(admin, null, false)
                context.stopService(Intent(context, BlockingVpnService::class.java))
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "VPN permanent : paquet introuvable", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "VPN permanent : échec", e)
            false
        }
    }

    /**
     * Verrous réseau associés au blocage (le VPN lui-même est piloté par [setNetworkBlocking]).
     * Chaque étape est isolée pour qu'un échec n'empêche ni les autres ni le LockTask.
     */
    private fun applyNetworkPolicies(dpm: DevicePolicyManager, admin: android.content.ComponentName) {
        // Empêcher de retirer le VPN, de partager la connexion ou de toucher aux réseaux mobiles.
        try {
            dpm.addUserRestriction(admin, UserManager.DISALLOW_CONFIG_VPN)
            dpm.addUserRestriction(admin, UserManager.DISALLOW_CONFIG_TETHERING)
            dpm.addUserRestriction(admin, UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)
        } catch (e: Exception) {
            Log.e(TAG, "Restrictions réseau : échec", e)
        }

        try {
            dpm.setApplicationHidden(admin, PLAY_STORE_PACKAGE, true)
        } catch (e: Exception) {
            Log.e(TAG, "Masquage du Play Store : échec", e)
        }

        // Reporter les mises à jour système (OTA constructeur) : pas de gros téléchargements.
        try {
            dpm.setSystemUpdatePolicy(admin, SystemUpdatePolicy.createPostponeInstallPolicy())
        } catch (e: Exception) {
            Log.e(TAG, "Politique de mise à jour système : échec", e)
        }
    }

    /** Lève le VPN permanent et arrête le VPN (avant déprovisionnement du Device Owner). */
    private fun removeNetworkPolicies(context: Context) {
        val dpm = dpm(context)
        val admin = KioskAdminReceiver.getComponentName(context)
        setNetworkBlocking(context, enabled = false, lockdown = false)
        try {
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_CONFIG_VPN)
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_CONFIG_TETHERING)
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)
            dpm.setApplicationHidden(admin, PLAY_STORE_PACKAGE, false)
            dpm.setSystemUpdatePolicy(admin, null)
        } catch (e: Exception) {
            Log.e(TAG, "Levée des politiques réseau : échec", e)
        }
    }

    /**
     * Vrai si le système autorise notre package en LockTask "LOCKED" (Device Owner + whitelist).
     * C'est la condition pour verrouiller SANS passer par l'épinglage d'écran de secours
     * (qui affiche le toast « This app can't be pinned » quand il est désactivé).
     */
    private fun isLockTaskPermitted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            dpm(context).isLockTaskPermitted(context.packageName)
        else
            isDeviceOwner(context)

    /** Active le mode kiosk (verrouille l'écran sur l'app). */
    fun startKiosk(activity: Activity) {
        try {
            // IMPORTANT : (ré)appliquer la whitelist JUSTE AVANT startLockTask garantit le
            // mode LOCKED (Device Owner) et non PINNED. En LOCKED, aucun toast
            // « This app can't be unpinned » n'est affiché.
            if (isDeviceOwner(activity)) {
                val dpm = dpm(activity)
                val admin = KioskAdminReceiver.getComponentName(activity)
                dpm.setLockTaskPackages(admin, LOCK_TASK_PACKAGES)
            }
            // Ne JAMAIS appeler startLockTask si le mode LOCKED n'est pas permis : Android
            // retomberait sur l'épinglage d'écran classique -> dialogue de confirmation ou
            // toast « This app can't be pinned ». Sans Device Owner, on n'essaie pas.
            if (!isLockTaskPermitted(activity)) {
                Log.w(TAG, "LockTask non permis (pas Device Owner ?) : kiosk non verrouillé.")
                return
            }
            if (!isLockTaskActive(activity)) {
                activity.startLockTask()
            }
        } catch (e: Exception) {
            Log.e(TAG, "startLockTask a échoué", e)
        }
    }

    /** Désactive le mode kiosk (réservé à l'admin après vérification du PIN). */
    fun stopKiosk(activity: Activity) {
        try {
            if (isLockTaskActive(activity)) {
                activity.stopLockTask()
            }
        } catch (e: Exception) {
            Log.e(TAG, "stopLockTask a échoué", e)
        }
    }

    /** Ouvre les Réglages WiFi (maintenance). À appeler après stopKiosk(). */
    fun openWifiSettings(context: Context) =
        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    /** Ouvre les Réglages Bluetooth (maintenance). À appeler après stopKiosk(). */
    fun openBluetoothSettings(context: Context) =
        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    /**
     * Retire complètement le Device Owner (déprovisionnement).
     * À utiliser uniquement pour désinstaller proprement le kiosk d'un pad.
     */
    fun clearDeviceOwner(context: Context) {
        if (isDeviceOwner(context)) {
            removeNetworkPolicies(context)
            dpm(context).clearDeviceOwnerApp(context.packageName)
        }
    }
}
