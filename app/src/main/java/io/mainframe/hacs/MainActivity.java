package io.mainframe.hacs;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

import io.mainframe.hacs.mqtt.MqttConnector;
import io.mainframe.hacs.mqtt.MqttConnectorCallbacks;
import io.mainframe.hacs.settings.SettingsActivity;
import io.mainframe.hacs.ssh.PkCredentials;
import io.mainframe.hacs.ssh.RunSshAsync;
import io.mainframe.hacs.ssh.SshResponse;



public class MainActivity extends AppCompatActivity implements View.OnClickListener, SshResponse<RunSshAsync.Result>, YesNoDialog.ResultListener, MqttConnectorCallbacks, NetworkStatus.NetworkStatusCallback {


    public static class DoorStateElement {
        // can be null for unkown
        private Status status;
        private String label;

        public DoorStateElement(Status status, String label) {
            this.status = status;
            this.label = label;
        }

        public Status getStatus() {
            return this.status;
        }

        @Override
        public String toString() {
            return this.label;
        }
    }


    private static final String TAG = MainActivity.class.getName();

    private NetworkStatus networkStatus = new NetworkStatus(this);
    private MqttConnector mqttConnector;
    private boolean isInMainframeWifi = false;
    private String tryCommand;

    private boolean skipNextOnSelectStatusNext = false;

    private final DoorStateElement[] spaceStatus = {
            new DoorStateElement(null, "Set Status"),
            new DoorStateElement(Status.OPEN_PLUS, "Open+"),
            new DoorStateElement(Status.OPEN, "Open"),
            new DoorStateElement(Status.MEMBER, "Member"),
            new DoorStateElement(Status.KEYHOLDER, "Keyholder"),
            new DoorStateElement(Status.CLOSE, "Close")
    };

    private final DoorStateElement[] spaceStatusNext = {
            new DoorStateElement(null, "Set next Status"),
            new DoorStateElement(Status.NOT_SET, "Not set"),
            new DoorStateElement(Status.OPEN_PLUS, "Open+"),
            new DoorStateElement(Status.OPEN, "Open"),
            new DoorStateElement(Status.MEMBER, "Member"),
            new DoorStateElement(Status.KEYHOLDER, "Keyholder"),
            new DoorStateElement(Status.CLOSE, "Close")
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        findViewById(R.id.reconnectMqttButton).setOnClickListener(this);

        final ArrayAdapter<DoorStateElement> statusNextAdapter = new DisableFirstEntryArrayAdapter<>(this, android.R.layout.simple_spinner_item, this.spaceStatusNext);
        statusNextAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        final Spinner spinnerNext = (Spinner) findViewById(R.id.spinnerStatusNext);
        spinnerNext.setAdapter(statusNextAdapter);
        spinnerNext.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (skipNextOnSelectStatusNext) {
                    skipNextOnSelectStatusNext = false;
                    return;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                System.out.println("nothing selected");
            }
        });


        final ArrayAdapter<DoorStateElement> statusAdapter = new DisableFirstEntryArrayAdapter<>(this, android.R.layout.simple_spinner_item, this.spaceStatus);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        final Spinner spinner = (Spinner) findViewById(R.id.spinnerStatusNow);
        spinner.setAdapter(statusAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    return;
                }

                startWaiting();
                final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                PkCredentials credentials = new PkCredentials(preferences);
                final DoorStateElement newState = MainActivity.this.spaceStatus[position];
                MainActivity.this.tryCommand = DoorCommand.getCmd(newState.getStatus());
                new RunSshAsync(MainActivity.this, MainActivity.this.tryCommand, true).execute(credentials);

                spinner.setSelection(0);

                if (spinnerNext.getSelectedItemPosition() != 0) {
                    // set next back to none
                    spinnerNext.setSelection(0);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                System.out.println("nothing selected");
            }
        });

        this.mqttConnector = new MqttConnector(getApplicationContext(), this);
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
        registerReceiver(this.networkStatus, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(this.networkStatus);
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
        switch (v.getId()) {
            case R.id.reconnectMqttButton:
                // prevents double clicking
                findViewById(R.id.reconnectMqttButton).setVisibility(View.INVISIBLE);
                this.mqttConnector.connect();
                return;
        }
    }


    /**
     * When a 'RunSshAsync' task is completed
     */
    @Override
    public void processFinish(RunSshAsync.Result response) {
        this.
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
        findViewById(R.id.spinnerStatusNow).setEnabled(false);
        findViewById(R.id.spinnerStatusNext).setEnabled(false);
        findViewById(R.id.progressMain).setVisibility(View.VISIBLE);
    }

    private void stopWaiting() {
        findViewById(R.id.spinnerStatusNow).setEnabled(true);
        findViewById(R.id.spinnerStatusNext).setEnabled(true);
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

        findViewById(R.id.spinnerStatusNow).setEnabled(enable);
        findViewById(R.id.verifiedWifi).setBackground(getResources().getDrawable(
                enable ? R.drawable.ic_verified_user_black_24dp: R.drawable.ic_error_black_24dp));
    }

    private void updateDoorStatus(String message, int color) {
        TextView textView = (TextView) findViewById(R.id.textStatus);
        textView.setTextColor(getResources().getColor(color));
        textView.setText(message);
    }

    private void updateGlobalWarnState(String message) {
        final View warnView = findViewById(R.id.globalWarnView);
        if (message == null || message.isEmpty()) {
            warnView.setVisibility(View.GONE);
        } else {
            TextView textView = (TextView) findViewById(R.id.globalWarnTextView);
            Log.w(TAG, message);
            textView.setText(message);
            warnView.setVisibility(View.VISIBLE);
        }
    }

    private void checkWifi() {
        this.isInMainframeWifi = false;
        Log.d(TAG, "Checking Wifi");
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo == null || !activeInfo.isConnected()) {
            Log.d(TAG, "No network connection.");
            return;
        }
        boolean requireMainframeWifi = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("requireMainframeWifi", true);
        if (!requireMainframeWifi) {
            this.isInMainframeWifi = true;
            Log.i(TAG, "requireMainframeWifi = false");
            return;
        }


        if (!(activeInfo.getType() == ConnectivityManager.TYPE_WIFI)) {
            Log.d(TAG, "No wifi connection.");
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
                return;
            }
        }

        Log.d(TAG, "Wrong wifi, you must connect to a mainframe wifi.");
    }

    /* mqtt callbacks */

    @Override
    public void onMqttReady() {
        updateDoorStatus("...", R.color.statusUnknown);
        findViewById(R.id.containerMqttConnection).setVisibility(View.INVISIBLE);

        ((TextView) findViewById(R.id.currentStatus)).setText("UNKNOWN");
        ((TextView) findViewById(R.id.nextStatus)).setText("");
    }

    @Override
    public void onMqttMessage(String topic, String msg) {
        if (Constants.MQTT_TOPIC_STATUS.equals(topic)) {
            updateStatusText(R.id.currentStatus, msg);

        } else if (Constants.MQTT_TOPIC_STATUS_NEXT.equals(topic)) {
            updateStatusText(R.id.nextStatus, msg);
        }
    }

    /* NetworkStatus callbacks */
    @Override
    public void onNetworkChange(boolean hasNetwork, boolean hasWifi, boolean hasMobile) {
        checkWifi();
        updateButtonState();
    }

    private void updateStatusText(@IdRes int id, String newStatusMsg) {
        TextView textView = (TextView) findViewById(id);
        if (newStatusMsg == null || newStatusMsg.isEmpty()) {
            textView.setText("");
            return;
        }

        try {
            Status newStatus = Status.byValue(newStatusMsg);
            textView.setText(newStatus.getValue());
        } catch (IllegalArgumentException e) {
            textView.setText("UNKNOWN");
        }
    }

    @Override
    public void error(String msg) {
        updateDoorStatus("mqtt error: " + msg, R.color.statusUnknown);
        findViewById(R.id.reconnectMqttButton).setVisibility(View.VISIBLE);
        findViewById(R.id.containerMqttConnection).setVisibility(View.VISIBLE);

        ((TextView) findViewById(R.id.currentStatus)).setText("UNKNOWN");
        ((TextView) findViewById(R.id.nextStatus)).setText("UNKNOWN");
    }
}
