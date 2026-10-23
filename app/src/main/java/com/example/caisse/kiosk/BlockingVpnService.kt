package com.example.caisse.kiosk

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.IOException

/**
 * VPN local « trou noir » : capte tout le trafic IPv4/IPv6 de l'appareil et le jette.
 *
 * Seule la caisse en est exclue ([addDisallowedApplication]) et garde donc le réseau normal
 * (Firestore, Storage, mises à jour silencieuses…). Toutes les autres applis (Play Services,
 * Play Store, synchros système…) n'ont plus de réseau, en 4G/3G comme en WiFi.
 *
 * Imposé comme VPN permanent par [KioskManager.applyKioskPolicies] (Device Owner) : c'est le
 * système qui démarre ce service, au boot et après chaque arrêt.
 */
class BlockingVpnService : VpnService() {

    companion object {
        private const val TAG = "BlockingVpnService"
    }

    private var tun: ParcelFileDescriptor? = null
    private var drainThread: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (tun == null) startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        try {
            val iface = Builder()
                .setSession("Caisse - blocage réseau")
                .addAddress("10.0.0.2", 32)
                .addAddress("fd00::2", 128)
                .addRoute("0.0.0.0", 0)
                .addRoute("::", 0)
                .addDisallowedApplication(packageName)
                .establish()
            if (iface == null) {
                // null = le VPN n'est pas autorisé (pas de consentement / pas Device Owner).
                Log.w(TAG, "establish() a renvoyé null : VPN non autorisé.")
                stopSelf()
                return
            }
            tun = iface
            drainThread = Thread({ drain(iface) }, "vpn-blackhole").apply {
                isDaemon = true
                start()
            }
            Log.i(TAG, "VPN trou noir actif.")
        } catch (e: Exception) {
            Log.e(TAG, "Impossible d'établir le VPN", e)
            stopSelf()
        }
    }

    /** Lit le tun en boucle et jette chaque paquet. S'arrête quand le tun est fermé. */
    private fun drain(iface: ParcelFileDescriptor) {
        val buffer = ByteArray(32767)
        try {
            FileInputStream(iface.fileDescriptor).use { input ->
                while (!Thread.currentThread().isInterrupted) {
                    if (input.read(buffer) < 0) break
                }
            }
        } catch (e: IOException) {
            // Normal à la fermeture du tun.
        }
    }

    private fun stopVpn() {
        drainThread?.interrupt()
        drainThread = null
        try {
            tun?.close()
        } catch (e: IOException) {
            Log.w(TAG, "Fermeture du tun", e)
        }
        tun = null
    }

    override fun onRevoke() {
        stopVpn()
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
