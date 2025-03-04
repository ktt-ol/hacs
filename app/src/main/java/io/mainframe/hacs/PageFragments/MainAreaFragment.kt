package io.mainframe.hacs.PageFragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import io.mainframe.hacs.R
import io.mainframe.hacs.common.Constants.SPACE_DOOR_BACK
import io.mainframe.hacs.common.Constants.SPACE_DOOR_FRONT
import io.mainframe.hacs.common.YesNoDialog
import io.mainframe.hacs.components.DoorButtons
import io.mainframe.hacs.main.BackDoorStatus
import io.mainframe.hacs.main.NetworkStatusListener
import io.mainframe.hacs.main.NetworkStatusValues
import io.mainframe.hacs.main.Status
import io.mainframe.hacs.ssh.DoorCommand
import io.mainframe.hacs.ssh.PkCredentials
import io.mainframe.hacs.status.StatusEvent
import io.mainframe.hacs.status.Subscription
import org.pmw.tinylog.Logger

class MainAreaFragment : BasePageFragment(), NetworkStatusListener {

    private var subscription: Subscription? = null
    private val doorButtonsFront: DoorButtons? get() = view?.findViewById(R.id.status_doorButtons_front)
    private val doorButtonsBack: DoorButtons? get() = view?.findViewById(R.id.status_doorButtons_back)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_status, container, false)?.apply {
            findViewById<DoorButtons>(R.id.status_doorButtons_front)?.setOnButtonClickListener { doorButton, _ ->
                if (doorButton.status == Status.CLOSE) {
                    // special action when the space is going to be closed
                    withBackDoorCheck {
                        withTrashCheck {
                            interaction.sendSshCommand(
                                SPACE_DOOR_FRONT,
                                DoorCommand.getSwitchDoorStateCmd(doorButton.status)
                            )
                        }
                    }

                } else {
                    interaction.sendSshCommand(
                        SPACE_DOOR_FRONT,
                        DoorCommand.getSwitchDoorStateCmd(doorButton.status)
                    )
                }
            }

            findViewById<DoorButtons>(R.id.status_doorButtons_back)?.setOnButtonClickListener { doorButton, _ ->
                interaction.sendSshCommand(
                    SPACE_DOOR_BACK,
                    DoorCommand.getSwitchDoorStateCmd(doorButton.status)
                )
            }
        }
    }

    private fun withTrashCheck(next: () -> Unit) {
        val trashSummaryForTomorrow = interaction.trashCalendar.getTrashSummaryForTomorrow()
        if (trashSummaryForTomorrow == null) {
            next()
            return
        }

        YesNoDialog.show(
            checkNotNull(context) { "Needs the context here" }, "Müll prüfen",
            "Morgen ist Müllabfuhr! Ist schon $trashSummaryForTomorrow an die Straße gestellt?",
            "trash"
        ) { _, resultOk ->
            if (resultOk) {
                next()
            }
        }
    }

    private fun withBackDoorCheck(next: () -> Unit) {
        if (getBackdoorStatus() != BackDoorStatus.OPEN) {
            next()
            return
        }

        YesNoDialog.show(
            checkNotNull(context) { "Needs the context here" },
            "Back-Door prüfen.",
            "Die Back-Door ist noch offen. Wirklich abschließen?",
            "bd"
        ) { _, resultOk ->
            if (resultOk) {
                next()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // if the ssh key password is not set
        val readOnlyMode = !PkCredentials.isPasswordSet(context!!)

        if (!readOnlyMode) {
            val networkStatus = interaction.networkStatus
            networkStatus.addListener(this)

            if (networkStatus.requireMainframeWifi) {
                doorButtonsFront?.isEnabled = networkStatus.hasMainAreaBssid
                doorButtonsBack?.isEnabled = networkStatus.hasMainAreaBssid

            } else {
                doorButtonsFront?.isEnabled = true
                doorButtonsBack?.isEnabled = true
            }
        } else {
            doorButtonsFront?.isEnabled = false
            doorButtonsBack?.isEnabled = false
        }

        val statusService = interaction.statusService
        subscription = statusService.subscribe { event: StatusEvent, value: String? ->
            if (value == null) return@subscribe
            if (event == StatusEvent.SPACE_STATUS) {
                activity?.runOnUiThread {
                    setStatusText(Status.byEventStatusValue(value))
                }
            }
        }

        setStatusText(statusService.getLastStatusValue(StatusEvent.SPACE_STATUS))
        setLedImage(getBackdoorStatus())
    }

    override fun onPause() {
        super.onPause()

        subscription?.unsubscribe()
        interaction.networkStatus.removeListener(this)
    }

    override val titleRes: Int get() = R.string.nav_status

    private fun setStatusText(status: Status?) {
        val text = view?.findViewById<TextView>(R.id.status_status) ?: return
        text.text = status?.uiValue ?: getString(R.string.unknown)
    }

    private fun getBackdoorStatus(): BackDoorStatus {
        return try {
            interaction.statusService.getLastValue(StatusEvent.BACKDOOR)
                ?.let { BackDoorStatus.byMqttValue(it) }
                ?: BackDoorStatus.UNKNOWN
        } catch (e: IllegalStateException) {
            Logger.warn("Unexpected Backdoor status: ${e.message}")
            BackDoorStatus.UNKNOWN
        }
    }

    private fun setLedImage(status: BackDoorStatus?) {
        val view = view ?: return

        val imageView = view.findViewById<ImageView>(R.id.back_door_status)
        when (status) {
            BackDoorStatus.OPEN -> imageView.setImageResource(R.drawable.ic_led_red_black)
            BackDoorStatus.CLOSED -> imageView.setImageResource(R.drawable.ic_led_blue_black)
            else -> imageView.setImageResource(R.drawable.ic_button_black)
        }
    }

    /* callback */

    override fun onNetworkChange(status: NetworkStatusValues) {
        doorButtonsFront?.isEnabled = status.hasMainAreaBssid
        doorButtonsBack?.isEnabled = status.hasMainAreaBssid
    }
}
