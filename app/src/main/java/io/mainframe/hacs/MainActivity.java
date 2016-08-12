package io.mainframe.hacs;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import io.mainframe.hacs.settings.SettingsActivity;
import io.mainframe.hacs.ssh.PkCredentials;
import io.mainframe.hacs.ssh.RunSshAsync;
import io.mainframe.hacs.ssh.SshResponse;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, SshResponse<RunSshAsync.Result>, YesNoDialog.ResultListener {

    private static final String TAG = MainActivity.class.toString();

    private boolean isInMainframeWifi = false;
    private String tryCommand;

    private static final int[] STATUS_BUTTON_IDS = new int[]{R.id.buttonStatusNone, R.id.buttonStatusPublic, R.id.buttonStatusOpen};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        for (int id : STATUS_BUTTON_IDS) {
            findViewById(id).setOnClickListener(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkWifi();
        updateButtonState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_settings) {

            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);

            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        PkCredentials credentials = new PkCredentials(preferences);
        switch (v.getId()) {
            case R.id.buttonStatusNone:
                this.tryCommand = DoorCommand.CLOSE;
                break;
            case R.id.buttonStatusPublic:
                this.tryCommand = DoorCommand.PUBLIC;
                break;
            case R.id.buttonStatusOpen:
                this.tryCommand = DoorCommand.OPEN;
                break;
        }

        startWaiting();
        new RunSshAsync(this, this.tryCommand, true).execute(credentials);
    }

    /**
     * When a 'RunSshAsync' task is completed
     */
    @Override
    public void processFinish(RunSshAsync.Result response) {
        stopWaiting();
        switch (response.status) {
            case SUCCESS:
                Toast.makeText(this, response.msg, Toast.LENGTH_LONG).show();
                break;
            case WRONG_HOST_KEY:
                final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                boolean checkServerFingerprint = preferences.getBoolean("checkServerFingerprint", true);
                if (checkServerFingerprint) {
                    Toast.makeText(this, response.msg, Toast.LENGTH_LONG).show();
                    break;
                }
                String dialogMsg = response.msg + "\nContinue?";
                YesNoDialog.create("Wrong Hostkey", dialogMsg).show(getSupportFragmentManager(), "hostkey," + response.command);
                break;
            case UNKNOWN_ERROR:
                Toast.makeText(this, response.msg, Toast.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    public void dialogClosed(String tag, boolean resultOk) {
        if (resultOk && tag.startsWith("hostkey")) {
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            PkCredentials credentials = new PkCredentials(preferences);
            new RunSshAsync(this, this.tryCommand, false).execute(credentials);
        }
    }

    private void startWaiting() {
        for (int id : STATUS_BUTTON_IDS) {
            findViewById(id).setEnabled(false);
        }
        findViewById(R.id.progressMain).setVisibility(View.VISIBLE);
    }

    private void stopWaiting() {
        for (int id : STATUS_BUTTON_IDS) {
            findViewById(id).setEnabled(true);
        }
        findViewById(R.id.progressMain).setVisibility(View.INVISIBLE);
    }

    private void updateButtonState() {
        boolean enable = false;
        if (this.isInMainframeWifi) {
            enable = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("credentialsOk", false);
            if (!enable) {
                updateGlobalWarnState("Please select your private key in the settings.");
            } else {
                updateGlobalWarnState(null);
            }
        }

        for (int id : STATUS_BUTTON_IDS) {
            findViewById(id).setEnabled(enable);
        }
    }

    private void updateDoorState(String message) {
        TextView textView = (TextView) findViewById(R.id.doorStatus);
        Log.d(TAG, "Door status: " + message);
        textView.setText(message);
    }

    private void updateGlobalWarnState(String message) {
        TextView textView = (TextView) findViewById(R.id.globalWarnTextView);
        if (message == null || message.isEmpty()) {
            textView.setVisibility(View.GONE);
        } else {
            Log.w(TAG, message);
            textView.setText(message);
            textView.setVisibility(View.VISIBLE);
        }
    }

    private void checkWifi() {
        this.isInMainframeWifi = false;
        Log.d(TAG, "Checking Wifi");
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo == null || !activeInfo.isConnected()) {
            updateDoorState("No network connection.");
            return;
        }
        boolean requireMainframeWifi = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("requireMainframeWifi", true);
        if (!requireMainframeWifi) {
            this.isInMainframeWifi = true;
            Log.i(TAG, "requireMainframeWifi = false");
            updateDoorState("Ignore");
            return;
        }


        if (!(activeInfo.getType() == ConnectivityManager.TYPE_WIFI)) {
            updateDoorState("No wifi connection.");
            return;
        }

        WifiManager wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();

        Log.d(TAG, String.format("Wifi found ssid: %s, bssid: %s", wifiInfo.getSSID(), wifiInfo.getBSSID()));
        for (String ssid : Constants.MAINFRAME_SSIDS) {
            String quotedSsid = "\"" + ssid + "\"";
            if (quotedSsid.equals(wifiInfo.getSSID())) {
                this.isInMainframeWifi = true;
                Log.i(TAG, "Mainframe wifi found.");
                updateDoorState("In Mainframe wifi.");
                return;
            }
        }

        updateDoorState("Wrong wifi, you must connect to a mainframe wifi.");
    }
}
