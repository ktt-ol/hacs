package io.mainframe.hacs.PageFragments

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.mainframe.hacs.R
import io.mainframe.hacs.common.Constants.SPACE_DOOR
import io.mainframe.hacs.main.NetworkStatusListener
import io.mainframe.hacs.main.NetworkStatusValues
import io.mainframe.hacs.main.Status
import io.mainframe.hacs.ssh.DoorCommand
import io.mainframe.hacs.ssh.PkCredentials.Companion.isPasswordSet
import io.mainframe.hacs.status.SpaceDevices
import io.mainframe.hacs.status.StatusEvent
import io.mainframe.hacs.status.Subscription


/**
 *
 */
class OverviewFragment : BasePageFragment(), NetworkStatusListener {
    private val locationColor = LocationColor()
    private var subscription: Subscription? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_overview, container, false)

        view.findViewById<View>(R.id.overview_change)
            .setOnClickListener { interaction.navigateToPage(MainAreaFragment::class.java) }
        view.findViewById<View>(R.id.overview_buzzer_outer).setOnClickListener {
            interaction.sendSshCommand(SPACE_DOOR, DoorCommand.outerDoorBuzzerCmd)
        }
        view.findViewById<View>(R.id.overview_buzzer_inner_glass).setOnClickListener {
            interaction.sendSshCommand(SPACE_DOOR, DoorCommand.innerGlassDoorBuzzerCmd)
        }
        view.findViewById<View>(R.id.overview_buzzer_inner_metal).setOnClickListener {
            interaction.sendSshCommand(SPACE_DOOR, DoorCommand.innerMetalDoorBuzzerCmd)
        }
        view.findViewById<View>(R.id.overview_become_keyholder)
            .setOnClickListener { // TODO: there should be an extra command to become keyholder
                val lastStatus =
                    interaction.statusService.getLastStatusValue(StatusEvent.SPACE_STATUS)
                if (lastStatus != null) {
                    interaction.sendSshCommand(
                        SPACE_DOOR,
                        DoorCommand.getSwitchDoorStateCmd(lastStatus)
                    )
                }
            }

        val trashInfoText = view.findViewById<TextView>(R.id.overview_trash_info)
        val trashInfo = interaction.trashCalendar.getTrashSummaryForTomorrow()
        if (trashInfo == null) {
            trashInfoText.visibility = View.GONE
        } else {
            trashInfoText.text = String.format(getString(R.string.overview_trashInfo), trashInfo)
            trashInfoText.visibility = View.VISIBLE
        }

        return view
    }

    override fun onResume() {
        super.onResume()

        val readOnlyMode = !isPasswordSet(context!!)
        if (!readOnlyMode) {
            val networkStatus = interaction.networkStatus
            networkStatus.addListener(this)

            if (networkStatus.requireMainframeWifi) {
                setButtonsEnabled(
                    buzzerEnabled = networkStatus.isInMainframeWifi,
                    becomeKeyholderEnabled = networkStatus.hasMainAreaBssid
                )
            } else {
                setButtonsEnabled(buzzerEnabled = true, becomeKeyholderEnabled = true)
            }
        } else {
            setButtonsEnabled(buzzerEnabled = false, becomeKeyholderEnabled = false)
        }

        val statusService = interaction.statusService
        this.subscription = statusService.subscribe { event: StatusEvent, value: String? ->
            if (value == null) return@subscribe
            checkNotNull(activity).runOnUiThread {
                when (event) {
                    StatusEvent.SPACE_STATUS -> setStatusText(Status.byEventStatusValue(value))
                    StatusEvent.KEYHOLDER -> setKeyholderText(value)
                    StatusEvent.DEVICES -> setDevicesText(SpaceDevices(value))
                    else -> Unit
                }
            }
        }

        setStatusText(statusService.getLastStatusValue(StatusEvent.SPACE_STATUS))
        setKeyholderText(statusService.getLastValue(StatusEvent.KEYHOLDER))
        val devices = statusService.getLastValue(StatusEvent.DEVICES)
        if (devices != null) {
            setDevicesText(SpaceDevices(devices))
        }
    }

    override fun onPause() {
        super.onPause()

        subscription?.let {
            it.unsubscribe()
            subscription = null
        }
        interaction.networkStatus.removeListener(this)
    }

    override val titleRes: Int get() = R.string.nav_overview

    private fun setStatusText(status: Status?) {
        val text = view?.findViewById<TextView>(R.id.overview_status) ?: return
        text.text = status?.uiValue ?: getString(R.string.unknown)
    }

    private fun setKeyholderText(keyholderText: String?) {
        val overview = view?.findViewById<TextView>(R.id.overview_keyholder) ?: return
        overview.text = when {
            keyholderText == null -> getString(R.string.unknown)
            keyholderText.isEmpty() -> getString(R.string.keyholder_no_one)
            else -> keyholderText
        }
    }

    private fun setDevicesText(devices: SpaceDevices?) {
        val devicesText = view?.findViewById<View>(R.id.overview_devices) as? TextView ?: return
        var formatted = "?"
        if (devices != null) {
            val buffer = StringBuilder()
            for (user in devices.users) {
                buffer.append("● ").append(user.name)
                if (user.devices.isNotEmpty()) {
                    buffer.append(" [")

                    val userDevices = user.devices
                    for (i in userDevices.indices) {
                        val device = userDevices[i]
                        val color = locationColor.getColor(device.location)
                        if (color != null) {
                            buffer.append(makeColorTag(device.name, color))
                        } else {
                            buffer.append(device.name)
                        }

                        if (i < userDevices.size - 1) {
                            buffer.append(", ")
                        }
                    }
                    buffer.append("]")
                }
                buffer.append("<br>\n")
            }
            buffer.append("<br>\nOrte: ")
            for (colorPair in locationColor.colors) {
                buffer.append(makeColorTag(colorPair.first, colorPair.second)).append(" ")
            }

            buffer.append("<br>\n<br>\n")
            buffer.append(getString(R.string.overview_anonPerson)).append(": ")
                .append(devices.anonPeople).append("<br>\n")
                .append(getString(R.string.overview_unknownDev)).append(": ")
                .append(devices.unknownDevices)

            buffer.append("<br><br><br><br>")
            formatted = buffer.toString()
        }
        devicesText.text = Html.fromHtml(formatted)
    }

    private fun makeColorTag(content: String, color: String): String {
        return "<font color='$color'>$content</font>"
    }

    private fun setButtonsEnabled(buzzerEnabled: Boolean, becomeKeyholderEnabled: Boolean) {
        view?.apply {
            findViewById<View>(R.id.overview_buzzer_outer).isEnabled = buzzerEnabled
            findViewById<View>(R.id.overview_buzzer_inner_glass).isEnabled = buzzerEnabled
            findViewById<View>(R.id.overview_buzzer_inner_metal).isEnabled = buzzerEnabled
            findViewById<View>(R.id.overview_become_keyholder).isEnabled = becomeKeyholderEnabled
        }
    }

    /* callback */
    override fun onNetworkChange(status: NetworkStatusValues) {
        if (status.requireMainframeWifi) {
            setButtonsEnabled(
                buzzerEnabled = status.isInMainframeWifi,
                becomeKeyholderEnabled = status.hasMainAreaBssid
            )
        } else {
            setButtonsEnabled(buzzerEnabled = true, becomeKeyholderEnabled = true)
        }
    }
}
