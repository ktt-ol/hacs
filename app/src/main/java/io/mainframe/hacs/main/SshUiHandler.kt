package io.mainframe.hacs.main

import android.app.Dialog
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import io.mainframe.hacs.R
import io.mainframe.hacs.common.Constants.DoorServer
import io.mainframe.hacs.common.YesNoDialog.show
import io.mainframe.hacs.ssh.DoorCommand
import io.mainframe.hacs.ssh.PkCredentials.Companion.fromSettings
import io.mainframe.hacs.ssh.RunSshAsync

/**
 * Submits ssh commands and shows a progress bar meanwhile.
 */
class SshUiHandler : DialogFragment() {
    private var tryServer: DoorServer? = null
    private var tryCommand: DoorCommand? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ssh_ui_handler, container, false)
    }

    /**
     * The system calls this only when creating the layout in a dialog.
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // The only reason you might override this method when using onCreateView() is
        // to modify any dialog characteristics. For example, the dialog includes a
        // title by default, but your custom layout might not need it. So here you can
        // remove the dialog title, but you must call the superclass to get the Dialog.
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        isCancelable = false
        return dialog
    }

    fun runSshCommand(
        server: DoorServer,
        command: DoorCommand,
        fragmentActivity: FragmentActivity
    ) {
        this.tryServer = server
        this.tryCommand = command

        show(fragmentActivity.supportFragmentManager, "dialog")

        val credentials = fromSettings(fragmentActivity)
        RunSshAsync(this::onSshFinish, server, credentials, command, true).execute()
    }

    /**
     * When a 'RunSshAsync' task is completed
     */
    private fun onSshFinish(response: RunSshAsync.Result) {
        val context = context ?: return
        when (response.status) {
            RunSshAsync.Status.SUCCESS -> {
                Toast.makeText(context, response.msg, Toast.LENGTH_LONG).show()
                actionDone(true)
            }

            RunSshAsync.Status.WRONG_HOST_KEY -> {
                actionDone(false)
                val checkServerFingerprint =
                    PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                        getString(R.string.PREFS_CHECK_SERVER_FINGERPRINT), true
                    )
                if (checkServerFingerprint) {
                    Toast.makeText(context, response.msg, Toast.LENGTH_LONG).show()
                    return
                }
                val dialogMsg = "${response.msg}\nContinue?"
                show(context, "Wrong Hostkey", dialogMsg, "hostkey") { tag, resultOk ->
                    if (resultOk && tag == "hostkey") {
                        // try the last command again
                        val credentials = fromSettings(context)
                        RunSshAsync(
                            this::onSshFinish,
                            checkNotNull(this.tryServer),
                            credentials,
                            checkNotNull(this.tryCommand),
                            false
                        ).execute()
                    } else {
                        actionDone(false)
                    }
                }
            }

            RunSshAsync.Status.UNKNOWN_ERROR -> {
                Toast.makeText(context, response.msg, Toast.LENGTH_LONG).show()
                actionDone(false)
            }
        }
    }

    private fun actionDone(result: Boolean) {
        dismissAllowingStateLoss()
    }
}
