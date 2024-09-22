package io.mainframe.hacs.main

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View
import io.mainframe.hacs.PageFragments.BasePageFragment
import io.mainframe.hacs.PageFragments.BasePageFragment.BasePageFragmentInteractionListener
import io.mainframe.hacs.PageFragments.CashboxFragment
import io.mainframe.hacs.PageFragments.MachiningFragment
import io.mainframe.hacs.PageFragments.OverviewFragment
import io.mainframe.hacs.PageFragments.StatusFragment
import io.mainframe.hacs.PageFragments.WoodworkingFragment
import io.mainframe.hacs.R
import io.mainframe.hacs.about.AboutActivity
import io.mainframe.hacs.common.Constants.DoorServer
import io.mainframe.hacs.log_view.LogViewerActivity
import io.mainframe.hacs.settings.SettingsActivity
import io.mainframe.hacs.ssh.DoorCommand
import io.mainframe.hacs.status.SpaceStatusService
import io.mainframe.hacs.trash_notifications.TrashCalendar
import org.pmw.tinylog.Logger
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks
import java.util.Stack

class MainActivity : AppCompatActivity(), PermissionCallbacks,
    NavigationView.OnNavigationItemSelectedListener, BasePageFragmentInteractionListener {

    private val permissions = arrayOf(
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
    private var fragmentBackState = Stack<Int>()

    private var _networkStatus: NetworkStatus? = null
    private var _trashCalendar: TrashCalendar? = null
    private var _statusService: SpaceStatusService? = null

    override val networkStatus: NetworkStatus get() = checkNotNull(_networkStatus)
    override val trashCalendar: TrashCalendar get() = checkNotNull(_trashCalendar)
    override val statusService: SpaceStatusService get() = checkNotNull(_statusService)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ensurePermission()) {
            afterPermissionInit(false)
        } else {
            return
        }

        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState == null) {
            selectBaseFragmentById(R.id.nav_overview, true)
        } else {
            val backState = savedInstanceState.getIntegerArrayList(BACK_STATE_KEY)
            if (backState != null) {
                fragmentBackState = Stack()
                fragmentBackState.addAll(backState)
            }
        }
    }

    private fun afterPermissionInit(afterPermission: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        _networkStatus = NetworkStatus(applicationContext, prefs)
        _statusService = SpaceStatusService()
        _trashCalendar = TrashCalendar { TrashCalendar.assetByContext(this, it) }

        setContentView(R.layout.activity_main)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        val toggle = ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        findViewById<NavigationView>(R.id.nav_view).setNavigationItemSelectedListener(this)

        // if the user gives us new permission we have to navigate to somewhere
        if (afterPermission) {
            selectBaseFragmentById(R.id.nav_overview, true)
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        outState.putIntegerArrayList(BACK_STATE_KEY, ArrayList(fragmentBackState))

        // call superclass to save any view hierarchy
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!fragmentBackState.isEmpty()) {
            fragmentBackState.pop()
        }

        if (fragmentBackState.isEmpty()) {
            super.onBackPressed()
            return
        }

        selectBaseFragmentById(fragmentBackState.peek(), false)
    }

    override fun setTitle(title: CharSequence) {
        findViewById<Toolbar>(R.id.toolbar)?.let {
            it.title = title
        }
    }

    private fun loadPageFragment(fragment: BasePageFragment) {
        title = getString(fragment.titleRes)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.content_frame, fragment)
            .commit()
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        selectBaseFragmentById(id, true)

        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    private fun selectBaseFragmentById(id: Int, addToBackStack: Boolean) {
        when (id) {
            R.id.nav_overview -> {
                loadPageFragment(OverviewFragment())
                if (addToBackStack) {
                    fragmentBackState.add(id)
                }
            }

            R.id.nav_status -> {
                loadPageFragment(StatusFragment())
                if (addToBackStack) {
                    fragmentBackState.add(id)
                }
            }

            R.id.nav_machining -> {
                loadPageFragment(MachiningFragment())
                if (addToBackStack) {
                    fragmentBackState.add(id)
                }
            }

            R.id.nav_woodworking -> {
                loadPageFragment(WoodworkingFragment())
                if (addToBackStack) {
                    fragmentBackState.add(id)
                }
            }

            R.id.nav_cashbox -> {
                loadPageFragment(CashboxFragment())
                if (addToBackStack) {
                    fragmentBackState.add(id)
                }
            }

            R.id.nav_abount -> {
                startActivity(Intent(this, AboutActivity::class.java))
            }

            R.id.nav_logs -> {
                startActivity(Intent(this, LogViewerActivity::class.java))
            }

            R.id.nav_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }

            else -> {
                Logger.warn("Unexpected id: $id")
                return
            }
        }

        findViewById<NavigationView>(R.id.nav_view).setCheckedItem(id)
    }

    override fun onResume() {
        super.onResume()

        networkStatus?.startListenOnConnectionChange()
        _statusService?.connect()
    }

    override fun onPause() {
        super.onPause()

        _statusService?.disconnect()
        networkStatus?.stopListenOnConnectionChange()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    private fun ensurePermission(): Boolean {
        if (EasyPermissions.hasPermissions(this, *permissions)) {
            return true
        } else {
            Logger.info("Requesting permission")
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.ask_for_permission),
                1,
                *permissions
            )
            return false
        }
    }

    /* EasyPermissions.PermissionCallbacks */
    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        Logger.warn("onPermissionsDenied: {} / {}", requestCode, perms.size)

        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this, getString(R.string.ask_for_permission_again))
                .setTitle(getString(R.string.app_name))
                .setPositiveButton(getString(R.string.action_settings))
                .setNegativeButton(getString(R.string.cancel), null /* click listener */)
                .setRequestCode(1)
                .build()
                .show()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        Logger.info("Permission ({}) granted, continue with init.", perms)
        if (perms.size != permissions.size) {
            Logger.warn("Got not all permissions, skipping init.")
            return
        }
        afterPermissionInit(true)
    }

    override fun sendSshCommand(server: DoorServer, command: DoorCommand) {
        SshUiHandler().runSshCommand(server, command, this)
    }


    override fun navigateToPage(target: Class<out BasePageFragment>) {
        if (StatusFragment::class.java == target) {
            selectBaseFragmentById(R.id.nav_status, true)
        }
    }

    companion object {
        const val BACK_STATE_KEY: String = "backsate"
    }
}
