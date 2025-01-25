package io.mainframe.hacs.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import io.mainframe.hacs.R
import io.mainframe.hacs.common.Constants
import org.pmw.tinylog.Logger
import java.util.Collections

class NetworkStatus(private val context: Context, prefs: SharedPreferences) :
    NetworkStatusValues, BroadcastReceiver(), OnSharedPreferenceChangeListener {
    private val allListener: MutableList<NetworkStatusListener> =
        Collections.synchronizedList(ArrayList())

    override var requireMainframeWifi: Boolean
        private set
    override var hasWifi = false
        private set
    override var hasMobile = false
        private set
    override var isInMainframeWifi = false
        private set
    override var hasMachiningBssid = false
        private set
    override var hasWoodworkingFrontBssid = false
        private set
    override var hasWoodworkingBackBssid = false
        private set


    override val hasNetwork: Boolean get() = hasWifi || hasMobile
    override val hasMainAreaBssid: Boolean
        get() = isInMainframeWifi && (!hasMachiningBssid && !hasWoodworkingFrontBssid && !hasWoodworkingBackBssid)

    init {
        requireMainframeWifi =
            prefs.getBoolean(context.getString(R.string.PREFS_REQUIRE_MAINFRAME_WIFI), true)
        prefs.registerOnSharedPreferenceChangeListener(this)
        parseResult(context)
    }

    fun startListenOnConnectionChange() {
        context.registerReceiver(this, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    fun stopListenOnConnectionChange() {
        context.unregisterReceiver(this)
    }

    fun addListener(listener: NetworkStatusListener) {
        allListener.add(listener)
    }

    fun removeListener(listener: NetworkStatusListener) {
        val iter = allListener.iterator()
        while (iter.hasNext()) {
            if (iter.next() === listener) {
                iter.remove()
                return
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        parseResult(context)
        Logger.debug(
            "onReceive ${intent.action}: hasNetwork=${hasNetwork}, hasMobile=$hasMobile, hasWifi=$hasWifi, isInMainframeWifi=$isInMainframeWifi"
        )
        updateListener()
    }

    private fun updateListener() {
        for (listener in allListener) {
            listener.onNetworkChange(this)
        }
    }

    private fun parseResult(context: Context) {
        hasWifi = false
        hasMobile = false
        isInMainframeWifi = false
        hasMachiningBssid = false
        hasWoodworkingFrontBssid = false
        hasWoodworkingBackBssid = false

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val allNetworks = cm.allNetworks
        for (network in allNetworks) {
            val ni = cm.getNetworkInfo(network) ?: continue

            if (ni.typeName.equals("WIFI", ignoreCase = true)) {
                if (ni.isConnected) {
                    hasWifi = true
                }
            } else if (ni.typeName.equals("MOBILE", ignoreCase = true)) {
                if (ni.isConnected) {
                    hasMobile = true
                }
            }
        }

        if (!hasWifi) {
            Logger.debug("No wifi connection.")
            return
        }

        val wifiMgr =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiMgr.connectionInfo
        Logger.debug("Wifi found ssid: {}, bssid: {}", wifiInfo.ssid, wifiInfo.bssid)

        if (!requireMainframeWifi) {
            this.isInMainframeWifi = true
            this.hasMachiningBssid = true
            this.hasWoodworkingFrontBssid = true
            this.hasWoodworkingBackBssid = true
            Logger.info("requireMainframeWifi = false")
            return
        } else {
            for (ssid in Constants.MAINFRAME_SSIDS) {
                val quotedSsid = "\"$ssid\""
                if (quotedSsid == wifiInfo.ssid) {
                    isInMainframeWifi = true
                    Logger.info("Mainframe wifi found.")
                    break
                }
            }

            if (this.isInMainframeWifi) {
                val bssid = wifiInfo.bssid

                this.hasMachiningBssid = hasMatchingBSSID(bssid, Constants.MACHINING_WIFI_BSSIDS)
                this.hasWoodworkingFrontBssid = hasMatchingBSSID(bssid, Constants.WOODWORKING_FRONT_WIFI_BSSIDS)
                this.hasWoodworkingBackBssid = hasMatchingBSSID(bssid, Constants.WOODWORKING_BACK_WIFI_BSSIDS)
            } else {
                Logger.debug("Wrong wifi, you must connect to a mainframe wifi.")
            }
        }
    }

    private fun hasMatchingBSSID(bssid: String, matchingIDs: Array<String>): Boolean {
        for (validBssid in matchingIDs) {
            if (validBssid.compareTo(bssid, ignoreCase = true) == 0) {
                return true
            }
        }
        return false
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        val prefKey = context.getString(R.string.PREFS_REQUIRE_MAINFRAME_WIFI)
        if (prefKey != key) {
            return
        }

        requireMainframeWifi = sharedPreferences.getBoolean(prefKey, true)
        parseResult(context)
        updateListener()
    }
}

interface NetworkStatusValues {
    val hasNetwork: Boolean
    val hasMobile: Boolean
    val hasWifi: Boolean
    val isInMainframeWifi: Boolean
    val hasMainAreaBssid: Boolean
    val hasMachiningBssid: Boolean
    val hasWoodworkingFrontBssid: Boolean
    val hasWoodworkingBackBssid: Boolean
    val requireMainframeWifi: Boolean
}

interface NetworkStatusListener {
    fun onNetworkChange(status: NetworkStatusValues)
}
