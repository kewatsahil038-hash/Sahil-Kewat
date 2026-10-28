package com.example.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build

data class DeviceStatusSnapshot(
    val batteryPercentage: Int,
    val isCharging: Boolean,
    val chargingType: String,
    val isConnected: Boolean,
    val networkType: String,
    val formattedSummaryHindi: String,
    val formattedSummaryEnglish: String
)

object DeviceStatusMonitor {

    fun getDeviceStatus(context: Context): DeviceStatusSnapshot {
        val batteryInfo = getBatteryInfo(context)
        val networkInfo = getNetworkInfo(context)

        val chargingHindi = when {
            batteryInfo.isCharging -> "aur phone charge ho raha hai (${batteryInfo.chargingSource})"
            batteryInfo.batteryPercent <= 15 -> "aur battery kam hai, kripya charger lagayein"
            else -> "aur charger nahi laga hai"
        }

        val chargingEnglish = when {
            batteryInfo.isCharging -> "and charging via ${batteryInfo.chargingSource}"
            batteryInfo.batteryPercent <= 15 -> "and battery is low, please connect a charger"
            else -> "not charging"
        }

        val networkHindi = if (networkInfo.isConnected) {
            "${networkInfo.type} internet connected hai"
        } else {
            "Internet connected nahi hai (offline)"
        }

        val networkEnglish = if (networkInfo.isConnected) {
            "Connected to ${networkInfo.type}"
        } else {
            "Device is offline"
        }

        val summaryHindi = "Phone ki battery ${batteryInfo.batteryPercent}% hai $chargingHindi. $networkHindi."
        val summaryEnglish = "Battery level is ${batteryInfo.batteryPercent}% ($chargingEnglish). $networkEnglish."

        return DeviceStatusSnapshot(
            batteryPercentage = batteryInfo.batteryPercent,
            isCharging = batteryInfo.isCharging,
            chargingType = batteryInfo.chargingSource,
            isConnected = networkInfo.isConnected,
            networkType = networkInfo.type,
            formattedSummaryHindi = summaryHindi,
            formattedSummaryEnglish = summaryEnglish
        )
    }

    private data class BatteryInfo(
        val batteryPercent: Int,
        val isCharging: Boolean,
        val chargingSource: String
    )

    private fun getBatteryInfo(context: Context): BatteryInfo {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, intentFilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        val batteryPercent = if (level != -1 && scale > 0) {
            (level * 100 / scale)
        } else {
            100
        }

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val chargingSource = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Adapter"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> if (isCharging) "Power Source" else "Battery"
        }

        return BatteryInfo(batteryPercent, isCharging, chargingSource)
    }

    private data class NetworkInfo(
        val isConnected: Boolean,
        val type: String
    )

    private fun getNetworkInfo(context: Context): NetworkInfo {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkInfo(false, "Offline")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = cm.activeNetwork ?: return NetworkInfo(false, "Offline")
            val capabilities = cm.getNetworkCapabilities(activeNetwork)
                ?: return NetworkInfo(false, "Offline")

            val isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

            val type = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> if (isConnected) "Active Network" else "Offline"
            }

            return NetworkInfo(isConnected, type)
        } else {
            @Suppress("DEPRECATION")
            val activeNetworkInfo = cm.activeNetworkInfo
            @Suppress("DEPRECATION")
            val isConnected = activeNetworkInfo?.isConnectedOrConnecting == true
            @Suppress("DEPRECATION")
            val type = when (activeNetworkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> "Wi-Fi"
                ConnectivityManager.TYPE_MOBILE -> "Mobile Data"
                else -> if (isConnected) "Network" else "Offline"
            }
            return NetworkInfo(isConnected, type)
        }
    }
}
