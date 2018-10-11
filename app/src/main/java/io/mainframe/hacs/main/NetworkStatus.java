package io.mainframe.hacs.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import io.mainframe.hacs.R;
import io.mainframe.hacs.common.Constants;

public class NetworkStatus extends BroadcastReceiver implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final Context context;

    private boolean requireMainframeWifi;
    private final List<NetworkStatusListener> allListener =
            Collections.synchronizedList(new ArrayList<NetworkStatusListener>());
    private boolean hasWifi, hasMobile, isInMainframeWifi, hasMachiningBssid;

    public NetworkStatus(Context ctx, SharedPreferences prefs) {
        context = ctx;
        requireMainframeWifi = prefs.getBoolean(ctx.getString(R.string.PREFS_REQUIRE_MAINFRAME_WIFI), true);
        prefs.registerOnSharedPreferenceChangeListener(this);
        parseResult(context);
    }

    public void startListenOnConnectionChange() {
        context.registerReceiver(this, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public void stopListenOnConnectionChange() {
        context.unregisterReceiver(this);
    }


    public boolean hasNetwork() {
        return hasWifi || hasMobile;
    }

    public boolean hasWifi() {
        return hasWifi;
    }

    public boolean hasMobile() {
        return hasMobile;
    }

    public boolean isInMainframeWifi() {
        return isInMainframeWifi;
    }

    public boolean hasMachiningBssid() {
        return hasMachiningBssid;
    }

    public void addListener(NetworkStatusListener listener) {
        allListener.add(listener);
    }

    public void removeListener(NetworkStatusListener listener) {
        final Iterator<NetworkStatusListener> iter = allListener.iterator();
        while (iter.hasNext()) {
            if (iter.next() == listener) {
                iter.remove();
                return;
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        parseResult(context);
        Logger.debug(String.format("onReceive %s: hasNetwork=%s, hasMobile=%s, hasWifi=%s, isInMainframeWifi=%s",
                intent.getAction(), hasNetwork(), hasMobile, hasWifi, isInMainframeWifi));
        updateListener();
    }

    private void updateListener() {
        for (NetworkStatusListener listener : allListener) {
            listener.onNetworkChange(hasNetwork(), hasMobile, hasWifi, isInMainframeWifi, hasMachiningBssid);
        }
    }

    private void parseResult(Context context) {
        hasWifi = false;
        hasMobile = false;
        isInMainframeWifi = false;
        hasMachiningBssid = false;

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final Network[] allNetworks = cm.getAllNetworks();
        for (Network network : allNetworks) {
            final NetworkInfo ni = cm.getNetworkInfo(network);

            if (ni.getTypeName().equalsIgnoreCase("WIFI")) {
                if (ni.isConnected()) {
                    hasWifi = true;
                }
            } else if (ni.getTypeName().equalsIgnoreCase("MOBILE")) {
                if (ni.isConnected()) {
                    hasMobile = true;
                }
            }
        }

        if (!hasWifi) {
            Logger.debug("No wifi connection.");
            return;
        }

        WifiManager wifiMgr = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        Logger.debug("Wifi found ssid: {}, bssid: {}", wifiInfo.getSSID(), wifiInfo.getBSSID());

        if (!requireMainframeWifi) {
            this.isInMainframeWifi = true;
            this.hasMachiningBssid = true;
            Logger.info("requireMainframeWifi = false");
            return;
        }

        for (String ssid : Constants.MAINFRAME_SSIDS) {
            String quotedSsid = "\"" + ssid + "\"";
            if (quotedSsid.equals(wifiInfo.getSSID())) {
                isInMainframeWifi = true;
                Logger.info("Mainframe wifi found.");
                break;
            }
        }

        if (this.isInMainframeWifi) {
            final String bssid = wifiInfo.getBSSID();
            for (String validBssid : Constants.MACHINING_WIFI_BSSIDS) {
                if (validBssid.compareToIgnoreCase(bssid) == 0) {
                    this.hasMachiningBssid = true;
                    break;
                }
            }
        } else {
            Logger.debug("Wrong wifi, you must connect to a mainframe wifi.");
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        final String prefKey = context.getString(R.string.PREFS_REQUIRE_MAINFRAME_WIFI);
        if (!prefKey.equals(key)) {
            return;
        }

        requireMainframeWifi = sharedPreferences.getBoolean(prefKey, true);
        parseResult(context);
        updateListener();
    }

    public interface NetworkStatusListener {
        void onNetworkChange(boolean hasNetwork, boolean hasMobile, boolean hasWifi, boolean isInMainframeWifi, boolean hasMachiningBssid);
    }
}
