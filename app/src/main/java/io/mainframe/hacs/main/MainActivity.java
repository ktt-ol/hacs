package io.mainframe.hacs.main;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.List;

import io.mainframe.hacs.PageFragments.BasePageFragment;
import io.mainframe.hacs.PageFragments.NextStatusFragment;
import io.mainframe.hacs.PageFragments.OverviewFragment;
import io.mainframe.hacs.PageFragments.StatusFragment;
import io.mainframe.hacs.R;
import io.mainframe.hacs.about.AboutActivity;
import io.mainframe.hacs.mqtt.MqttConnector;
import io.mainframe.hacs.mqtt.MqttStatusListener;
import io.mainframe.hacs.settings.SettingsActivity;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends AppCompatActivity implements SshUiHandler.OnShhCommandHandler,
        EasyPermissions.PermissionCallbacks,
        NavigationView.OnNavigationItemSelectedListener, BasePageFragment.BasePageFragmentInteractionListener, MqttStatusListener {


    private static final String TAG = MainActivity.class.getName();

    private NetworkStatus networkStatus = null;
    private MqttConnector mqttConnector;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        networkStatus = new NetworkStatus(prefs.getBoolean("requireMainframeWifi", true));
        mqttConnector = new MqttConnector(getApplicationContext(), prefs);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        findViewById(R.id.mqttReconnect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mqttConnector.connect();
                findViewById(R.id.mqttOverlay).setVisibility(View.GONE);
            }
        });

        ensurePermission();

        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState == null) {
            navigationView.setCheckedItem(R.id.nav_overview);
            loadPageFragment(new OverviewFragment());
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        ((Toolbar) findViewById(R.id.toolbar)).setTitle(title);

    }

    private void loadPageFragment(BasePageFragment fragment) {
        setTitle(getString(fragment.getTitleRes()));
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
    }


    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

//        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        System.out.println("### onNavigationItemSelected" + id);

        if (id == R.id.nav_overview) {
            loadPageFragment(new OverviewFragment());
        } else if (id == R.id.nav_status) {
            loadPageFragment(new StatusFragment());
        } else if (id == R.id.nav_statusNext) {
            loadPageFragment(new NextStatusFragment());
        } else if (id == R.id.nav_abount) {
            startActivity(new Intent(this, AboutActivity.class));
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(this.networkStatus, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        mqttConnector.connect();
        mqttConnector.addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mqttConnector.removeListener(this);
        mqttConnector.disconnect();
        unregisterReceiver(this.networkStatus);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    private void ensurePermission() {
        String[] perms = {
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.READ_PHONE_STATE
        };
        if (!EasyPermissions.hasPermissions(this, perms)) {
            Log.w(TAG, "Requesting permission");
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, getString(R.string.ask_for_permission), 1, perms);
        }
    }

    /* mqtt callbacks */

    @Override
    public void onNewStatus(Topic topic, Status newStatus) {
        // ignored
    }

    @Override
    public void onMqttConnected() {
        findViewById(R.id.mqttOverlay).setVisibility(View.GONE);
    }


    @Override
    public void onMqttConnectionLost() {
        findViewById(R.id.mqttOverlay).setVisibility(View.VISIBLE);
    }


    /* EasyPermissions.PermissionCallbacks */

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());

        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this, getString(R.string.ask_for_permission_again))
                    .setTitle(getString(R.string.app_name))
                    .setPositiveButton(getString(R.string.action_settings))
                    .setNegativeButton(getString(R.string.cancel), null /* click listener */)
                    .setRequestCode(1)
                    .build()
                    .show();
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        // ignored
    }


    @Override
    public void onSshCommandComplete(String command, boolean success) {
        // ignore
    }


    @Override
    public void sendSshCommand(String command) {
        new SshUiHandler().runSshCommand(command, this);
    }

    @Override
    public NetworkStatus getNetworkStatus() {
        return networkStatus;
    }

    @Override
    public MqttConnector getMqttConnector() {
        return mqttConnector;
    }

    @Override
    public void navigateToPage(Class<? extends BasePageFragment> target) {
        if (StatusFragment.class == target) {
            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            navigationView.setCheckedItem(R.id.nav_status);
            loadPageFragment(new StatusFragment());
        }
    }
}
