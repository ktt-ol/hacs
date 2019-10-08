package io.mainframe.hacs.PageFragments

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import io.mainframe.hacs.R
import io.mainframe.hacs.common.Constants.SPACE_DOOR
import io.mainframe.hacs.common.YesNoDialog
import io.mainframe.hacs.components.DoorButtons
import io.mainframe.hacs.main.BackDoorStatus
import io.mainframe.hacs.main.NetworkStatus
import io.mainframe.hacs.main.Status
import io.mainframe.hacs.mqtt.MqttStatusListener
import io.mainframe.hacs.ssh.DoorCommand
import io.mainframe.hacs.ssh.PkCredentials
import io.mainframe.hacs.trash_notifications.TrashCalendar
import java.util.*

class StatusFragment : BasePageFragment(), NetworkStatus.NetworkStatusListener, MqttStatusListener {

    private lateinit var doorButtons: DoorButtons

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_status, container, false)

        doorButtons = view.findViewById(R.id.status_doorButtons)
        doorButtons.setOnButtonClickListener { doorButton, _ ->
            if (doorButton.status == Status.CLOSE) {
                // special action when the space is going to be closed
                withBackDoorCheck {
                    withTrashCheck {
                        interaction.sendSshCommand(SPACE_DOOR, DoorCommand.getSwitchDoorStateCmd(doorButton.status))
                    }
                }

            } else {
                interaction.sendSshCommand(SPACE_DOOR, DoorCommand.getSwitchDoorStateCmd(doorButton.status))
            }
        }

        return view
    }

    private fun withTrashCheck(next: () -> Unit) {
        val trashSummaryForTomorrow = TrashCalendar(context).trashSummaryForTomorrow
        if (trashSummaryForTomorrow == null) {
            next()
            return
        }

        YesNoDialog.show(context, "Müll prüfen",
                String.format("Morgen ist Müllabfuhr! Ist schon %s an die Straße gestellt?", trashSummaryForTomorrow),
                "trash") { _, resultOk ->
            if (resultOk) {
                next()
            }
        }
    }

    private fun withBackDoorCheck(next: () -> Unit) {
        val backDoorStatus = interaction.mqttConnector.getLastValue<BackDoorStatus>(MqttStatusListener.Topic.BACK_DOOR_BOLT, BackDoorStatus::class.java)
        if (backDoorStatus != BackDoorStatus.OPEN) {
            next()
            return
        }

        YesNoDialog.show(context, "Back-Door prüfen.", "Die Back-Door ist noch offen. Wirklich abschließen?", "bd") { tag, resultOk ->
            if (resultOk) {
                next()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val credentials = PkCredentials(PreferenceManager.getDefaultSharedPreferences(activity), context!!)
        // if the ssh key password is not set
        val readOnlyMode = !credentials.isPasswordSet

        if (!readOnlyMode) {
            val networkStatus = interaction.networkStatus
            networkStatus.addListener(this)

            if (networkStatus.isRequireMainframeWifi) {
                doorButtons.isEnabled = networkStatus.isInMainframeWifi && !networkStatus.hasMachiningBssid()
            } else {
                doorButtons.isEnabled = true
            }
        } else {
            doorButtons.isEnabled = false
        }

        val mqtt = interaction.mqttConnector
        mqtt.addListener(this, EnumSet.of<MqttStatusListener.Topic>(MqttStatusListener.Topic.STATUS, MqttStatusListener.Topic.BACK_DOOR_BOLT))

        setStatusText(mqtt.getLastValue<Status>(MqttStatusListener.Topic.STATUS, Status::class.java))
        setLedImage(mqtt.getLastValue(MqttStatusListener.Topic.BACK_DOOR_BOLT, BackDoorStatus::class.java))
    }

    override fun onPause() {
        super.onPause()

        interaction.mqttConnector.removeAllListener(this)
        interaction.networkStatus.removeListener(this)
    }

    override fun getTitleRes(): Int {
        return R.string.nav_status
    }

    private fun setStatusText(status: Status?) {
        val view = view ?: return
        val text = view.findViewById<TextView>(R.id.status_status)
        text.text = if (status == null) getString(R.string.unknown) else status.uiValue
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

    override fun onNetworkChange(hasNetwork: Boolean, hasMobile: Boolean, hasWifi: Boolean,
                                 isInMainframeWifi: Boolean, hasMachiningBssid: Boolean, requireMainframeWifi: Boolean) {
        doorButtons.isEnabled = !requireMainframeWifi || isInMainframeWifi
    }

    override fun onNewMsg(topic: MqttStatusListener.Topic, msg: Any) {
        if (topic == MqttStatusListener.Topic.STATUS) {
            setStatusText(msg as Status)
        } else if (topic == MqttStatusListener.Topic.BACK_DOOR_BOLT) {
            setLedImage(msg as BackDoorStatus)
        }
    }

    override fun onMqttConnected() {
        // not needed
    }

    override fun onMqttConnectionLost() {
        setStatusText(null)
        setLedImage(BackDoorStatus.UNKNOWN)
    }
}
