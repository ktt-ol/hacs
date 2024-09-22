package io.mainframe.hacs.PageFragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.mainframe.hacs.R
import io.mainframe.hacs.common.Constants.MACHINING_DOOR
import io.mainframe.hacs.components.DoorButtons
import io.mainframe.hacs.main.NetworkStatusListener
import io.mainframe.hacs.main.NetworkStatusValues
import io.mainframe.hacs.main.Status
import io.mainframe.hacs.ssh.DoorCommand
import io.mainframe.hacs.ssh.PkCredentials.Companion.isPasswordSet
import io.mainframe.hacs.status.StatusEvent
import io.mainframe.hacs.status.Subscription

/**
 * Created by holger on 21.05.18.
 */
class MachiningFragment : BasePageFragment(), NetworkStatusListener {
    private var subscription: Subscription? = null

    private val doorButtons: DoorButtons? get() = view?.findViewById(R.id.machining_doorButtons)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_machining, container, false)

        view.findViewById<DoorButtons>(R.id.machining_doorButtons)
            ?.setOnButtonClickListener { doorButton, _ ->
                interaction.sendSshCommand(
                    MACHINING_DOOR,
                    DoorCommand.getSwitchDoorStateCmd(doorButton.status)
                )
            }
        return view
    }

    override fun onResume() {
        super.onResume()

        // if the ssh key password is not set
        val readOnlyMode = !isPasswordSet(context!!)

        if (!readOnlyMode) {
            val networkStatus = interaction.networkStatus
            networkStatus.addListener(this)

            if (networkStatus.requireMainframeWifi) {
                doorButtons?.isEnabled = networkStatus.hasMachiningBssid
            } else {
                doorButtons?.isEnabled = true
            }
        } else {
            doorButtons?.isEnabled = false
        }

        val statusService = interaction.statusService
        this.subscription = statusService.subscribe { event: StatusEvent, value: String? ->
            if (value == null) return@subscribe
            checkNotNull(activity).runOnUiThread {
                if (event == StatusEvent.STATUS_MACHINING) {
                    setStatusText(Status.byEventStatusValue(value))
                }
            }
        }
        setStatusText(statusService.getLastStatusValue(StatusEvent.STATUS_MACHINING))
        setKeyholderText(statusService.getLastValue(StatusEvent.KEYHOLDER_MACHINING))
    }

    override fun onPause() {
        super.onPause()
        subscription?.let {
            it.unsubscribe()
            subscription = null
        }
    }

    override val titleRes: Int get() = R.string.nav_machining

    private fun setStatusText(status: Status?) {
        view?.findViewById<TextView>(R.id.machining_status)
            ?.text = status?.uiValue ?: getString(R.string.unknown)
    }

    private fun setKeyholderText(keyholderText: String?) {
        val keyholder = view?.findViewById<TextView>(R.id.machining_keyholder) ?: return
        keyholder.text = when {
            keyholderText == null -> getString(R.string.unknown)
            keyholderText.isEmpty() -> getString(R.string.keyholder_no_one)
            else -> keyholderText
        }
    }

    /* callback */
    override fun onNetworkChange(status: NetworkStatusValues) {
        if (status.requireMainframeWifi) {
            doorButtons?.isEnabled = status.hasMachiningBssid
        } else {
            doorButtons?.isEnabled = true
        }
    }
}
