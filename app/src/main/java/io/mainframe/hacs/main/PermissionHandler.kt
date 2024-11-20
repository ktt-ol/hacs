package io.mainframe.hacs.main

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.Toast
import io.mainframe.hacs.R
import org.pmw.tinylog.Logger

object PermissionHandler {

    data class State(
        val activity: Activity,
        val missingPermissions: List<String>
    )

    private val neededPermissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )

    /** Checks that all [neededPermissions] are granted or starts the permission request process. */
    fun ensurePermission(activity: Activity): Boolean {
        val missingPermissions = neededPermissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            return true
        }

        showPermissionInfoMessage(State(activity, missingPermissions))
        return false
    }


    private fun askForPermission(state: State) {
        Logger.info("Asking for permissions: ${state.missingPermissions}")
        ActivityCompat.requestPermissions(
            state.activity,
            state.missingPermissions.toTypedArray(),
            PERMISSION_CODE
        )
    }

    private fun showPermissionInfoMessage(state: State) {
        val message = state.activity.getString(R.string.ask_for_permission)

        AlertDialog.Builder(state.activity)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                askForPermission(state)
            }
            .show()
    }

    /** returns `true` if all permissions were granted, `false` for missing grants and `null` for not our request. */
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        activity: Activity
    ): Boolean? {
        if (requestCode != PERMISSION_CODE) {
            return null
        }

        val deniedPermissions = grantResults.mapIndexed { index, result ->
            if (result != PackageManager.PERMISSION_GRANTED) {
                permissions[index]
            } else {
                null
            }
        }.filterNotNull()

        return if (deniedPermissions.isEmpty()) {
            Logger.info("Permissions granted: ${permissions.joinToString()}")
            true
        } else {
            Logger.error("Permissions denied: ${deniedPermissions.joinToString()}")
            val msg = activity.getString(R.string.ask_for_permission_again)
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
            false
        }
    }

    private const val PERMISSION_CODE = 1
}
