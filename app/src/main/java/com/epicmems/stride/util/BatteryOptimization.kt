package com.epicmems.stride.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/**
 * Battery-optimisation exemption helpers.
 *
 * Exempting Stride from battery optimisation does two things on modern Android:
 *  - the system is far less likely to kill the tracking service, and
 *  - it lifts the Android 12+ restriction on starting a foreground service from the
 *    background, which is what lets the watchdog bring counting back on aggressive OEMs.
 */
object BatteryOptimization {

    fun isExempt(context: Context): Boolean = runCatching {
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        power.isIgnoringBatteryOptimizations(context.packageName)
    }.getOrDefault(false)

    /**
     * The direct "ignore battery optimisations?" dialog. Falls back to the full settings list
     * if the device doesn't support the direct request.
     */
    @SuppressLint("BatteryLife")
    fun requestIntent(context: Context): Intent {
        val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val resolvable = context.packageManager.resolveActivity(direct, 0) != null
        return if (resolvable) {
            direct
        } else {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
