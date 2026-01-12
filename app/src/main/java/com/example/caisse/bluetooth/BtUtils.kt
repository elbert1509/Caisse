package com.example.caisse.bluetooth

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

 fun requiredBtPerms(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
    else
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

 fun hasAllBtPermissions(ctx: android.content.Context): Boolean =
    requiredBtPerms().all {
        ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED
    }

 fun safeRun(action: () -> Unit) {
    try { action() } catch (_: SecurityException) { /* ignore/log if needed */ }
}
