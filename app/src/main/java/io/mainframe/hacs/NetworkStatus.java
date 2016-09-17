package io.mainframe.hacs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * Created by holger on 17.08.16.
 */
public class NetworkStatus extends BroadcastReceiver {

    private static final String TAG = NetworkStatus.class.getName();

    private static boolean hasWifi, hasMobile;

    private final NetworkStatusCallback callback;

    public static boolean hasNetwork() {
        return hasWifi || hasMobile;
    }

    public static boolean hasWifi() {
        return hasWifi;
    }

    public static boolean hasMobile() {
        return hasMobile;
    }

    public NetworkStatus(NetworkStatusCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        parseResult(context);
        Log.d(TAG, "onReceive " + intent.getAction() +
                "hasMobile=" + hasMobile() +
                ", hasNetwork=" + hasNetwork() +
                ", hasWifi=" + hasWifi());
        if (this.callback != null) {
            this.callback.onNetworkChange(hasNetwork(), hasWifi(), hasMobile());
        }
    }

    private void parseResult(Context context) {
        hasWifi = false;
        hasMobile = false;

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI")) {
                if (ni.isConnected()) {
                    hasWifi = true;
                    continue;
                }
            }
            if (ni.getTypeName().equalsIgnoreCase("MOBILE")) {
                if (ni.isConnected()) {
                    hasMobile = true;
                    continue;
                }
            }
        }
    }


    interface NetworkStatusCallback {
        void onNetworkChange(boolean hasNetwork, boolean hasWifi, boolean hasMobile);
    }
}
