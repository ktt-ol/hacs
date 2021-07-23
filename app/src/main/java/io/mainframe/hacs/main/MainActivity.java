package io.mainframe.hacs.main;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Stack;

import io.mainframe.hacs.PageFragments.BasePageFragment;
import io.mainframe.hacs.PageFragments.CashboxFragment;
import io.mainframe.hacs.PageFragments.MachiningFragment;
import io.mainframe.hacs.PageFragments.NextStatusFragment;
import io.mainframe.hacs.PageFragments.OverviewFragment;
import io.mainframe.hacs.PageFragments.StatusFragment;
import io.mainframe.hacs.R;
import io.mainframe.hacs.about.AboutActivity;
import io.mainframe.hacs.common.Constants;
import io.mainframe.hacs.log_view.LogViewerActivity;
import io.mainframe.hacs.mqtt.MqttConnector;
import io.mainframe.hacs.mqtt.MqttStatusListener;
import io.mainframe.hacs.settings.SettingsActivity;
import io.mainframe.hacs.ssh.DoorCommand;
import io.mainframe.hacs.trash_notifications.TrashCalendar;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends AppCompatActivity implements SshUiHandler.OnShhCommandHandler,
        EasyPermissions.PermissionCallbacks,
        NavigationView.OnNavigationItemSelectedListener, BasePageFragment.BasePageFragmentInteractionListener, MqttStatusListener {

    public static final String BACK_STATE_KEY = "backsate";

    private NetworkStatus networkStatus = null;
    private MqttConnector mqttConnector;

    private Stack<Integer> fragmentBackState = new Stack<>();

    private TrashCalendar trashCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        networkStatus = new NetworkStatus(getApplicationContext(), prefs);
        mqttConnector = new MqttConnector(getApplicationContext(), prefs);
        trashCalendar = new TrashCalendar(this);

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
            selectBaseFragmentById(R.id.nav_overview, true);
        } else {
            ArrayList<Integer> backState = savedInstanceState.getIntegerArrayList(BACK_STATE_KEY);
            if (backState != null) {
                fragmentBackState = new Stack<>();
                fragmentBackState.addAll(backState);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putIntegerArrayList(BACK_STATE_KEY, new ArrayList<>(fragmentBackState));

        // call superclass to save any view hierarchy
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (!fragmentBackState.isEmpty()) {
            fragmentBackState.pop();
        }

        if (fragmentBackState.isEmpty()) {
            super.onBackPressed();
            return;
        }

        selectBaseFragmentById(fragmentBackState.peek(), false);
    }

    @Override
    public void setTitle(CharSequence title) {
        ((Toolbar) findViewById(R.id.toolbar)).setTitle(title);
    }

    private void loadPageFragment(BasePageFragment fragment) {
        setTitle(getString(fragment.getTitleRes()));
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
    }


    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        selectBaseFragmentById(id, true);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void selectBaseFragmentById(int id, boolean addToBackStack) {
        if (id == R.id.nav_overview) {
            loadPageFragment(new OverviewFragment());
            if (addToBackStack) {
                fragmentBackState.add(id);
            }
        } else if (id == R.id.nav_status) {
            loadPageFragment(new StatusFragment());
            if (addToBackStack) {
                fragmentBackState.add(id);
            }
        } else if (id == R.id.nav_statusNext) {
            loadPageFragment(new NextStatusFragment());
            if (addToBackStack) {
                fragmentBackState.add(id);
            }
        } else if (id == R.id.nav_machining) {
            loadPageFragment(new MachiningFragment());
            if (addToBackStack) {
                fragmentBackState.add(id);
            }
        } else if (id == R.id.nav_cashbox) {
            loadPageFragment(new CashboxFragment());
            if (addToBackStack) {
                fragmentBackState.add(id);
            }
        } else if (id == R.id.nav_abount) {
            startActivity(new Intent(this, AboutActivity.class));
        } else if (id == R.id.nav_logs) {
            startActivity(new Intent(this, LogViewerActivity.class));
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else {
            Logger.warn("Unexpected id: " + id);
            return;
        }

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setCheckedItem(id);
    }

    @Override
    protected void onResume() {
        super.onResume();

        networkStatus.startListenOnConnectionChange();
        mqttConnector.connect();
        mqttConnector.addListener(this, EnumSet.noneOf(Topic.class));
    }

    @Override
    protected void onPause() {
        super.onPause();

        mqttConnector.removeAllListener(this);
        mqttConnector.disconnect();
        networkStatus.stopListenOnConnectionChange();
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
                Manifest.permission.WAKE_LOCK,
//                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.RECEIVE_BOOT_COMPLETED,
                // to get the ssid
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
        };
        if (!EasyPermissions.hasPermissions(this, perms)) {
            Logger.warn("Requesting permission");
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, getString(R.string.ask_for_permission), 1, perms);
        }
    }

    /* mqtt callbacks */

    @Override
    public void onNewMsg(Topic topic, Object msg) {
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
        Logger.warn("onPermissionsDenied:" + requestCode + ":" + perms.size());

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
    public void onSshCommandComplete(DoorCommand command, boolean success) {
        // ignore
    }


    @Override
    public void sendSshCommand(Constants.DoorServer server, DoorCommand command) {
        new SshUiHandler().runSshCommand(server, command, this);
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
            selectBaseFragmentById(R.id.nav_status, true);
        }
    }

    @Override
    public TrashCalendar getTrashCalendar() {
        return trashCalendar;
    }
}
