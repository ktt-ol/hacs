package io.mainframe.hacs.PageFragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.mainframe.hacs.R
import io.mainframe.hacs.common.Constants.WOODWORKING_DOOR_BACK
import io.mainframe.hacs.common.Constants.WOODWORKING_DOOR_FRONT
import io.mainframe.hacs.components.DoorButtons
import io.mainframe.hacs.main.NetworkStatusListener
import io.mainframe.hacs.main.NetworkStatusValues
import io.mainframe.hacs.main.Status
import io.mainframe.hacs.ssh.DoorCommand
import io.mainframe.hacs.ssh.PkCredentials.Companion.isPasswordSet
import io.mainframe.hacs.status.StatusEvent
import io.mainframe.hacs.status.Subscription

/**
 * Aka Holzwerkstatt
 */
class WoodworkingFragment : BasePageFragment(), NetworkStatusListener {
    private var subscription: Subscription? = null

    private val doorButtonsFront: DoorButtons? get() = view?.findViewById(R.id.woodworking_doorButtons_front)
    private val doorButtonsBack: DoorButtons? get() = view?.findViewById(R.id.woodworking_doorButtons_back)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_woodworking, container, false)
        view.findViewById<DoorButtons>(R.id.woodworking_doorButtons_front)
            .setOnButtonClickListener { doorButton, _ ->
                interaction.sendSshCommand(WOODWORKING_DOOR_FRONT, DoorCommand.getSwitchDoorStateCmd(doorButton.status))
            }
        view.findViewById<DoorButtons>(R.id.woodworking_doorButtons_back)
            .setOnButtonClickListener { doorButton, _ ->
                interaction.sendSshCommand(WOODWORKING_DOOR_BACK, DoorCommand.getSwitchDoorStateCmd(doorButton.status))
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
                doorButtonsFront?.isEnabled = networkStatus.hasWoodworkingFrontBssid
                doorButtonsBack?.isEnabled = networkStatus.hasWoodworkingBackBssid
            } else {
                doorButtonsFront?.isEnabled = true
                doorButtonsBack?.isEnabled = true
            }
        } else {
            doorButtonsFront?.isEnabled = false
            doorButtonsBack?.isEnabled = false
        }

        val statusService = interaction.statusService
        this.subscription = statusService.subscribe { event: StatusEvent, value: String? ->
            if (value == null) return@subscribe
            checkNotNull(activity).runOnUiThread {
                if (event == StatusEvent.STATUS_WOODWORKING) {
                    setStatusText(Status.byEventStatusValue(value))
                }
            }
        }
        setStatusText(statusService.getLastStatusValue(StatusEvent.STATUS_WOODWORKING))
        setKeyholderText(statusService.getLastValue(StatusEvent.KEYHOLDER_WOODWORKING))
    }

    override fun onPause() {
        super.onPause()
        subscription?.let {
            it.unsubscribe()
            subscription = null
        }
    }

    override val titleRes: Int get() = R.string.nav_woodworking

    private fun setStatusText(status: Status?) {
        val text = view?.findViewById<TextView>(R.id.woodworking_status) ?: return
        text.text = status?.uiValue ?: getString(R.string.unknown)
    }

    private fun setKeyholderText(keyholderText: String?) {
        val keyholder =
            (view?.findViewById<View>(R.id.woodworking_keyholder) as? TextView) ?: return
        keyholder.text = when {
            keyholderText == null -> getString(R.string.unknown)
            keyholderText.isEmpty() -> getString(R.string.keyholder_no_one)
            else -> keyholderText
        }
    }

    /* callback */
    override fun onNetworkChange(status: NetworkStatusValues) {
        if (status.requireMainframeWifi) {
            doorButtonsFront?.isEnabled = status.hasWoodworkingFrontBssid
            doorButtonsBack?.isEnabled = status.hasWoodworkingBackBssid
        } else {
            doorButtonsFront?.isEnabled = true
            doorButtonsBack?.isEnabled = true
        }
    }
}
