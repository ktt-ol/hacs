package io.mainframe.hacs.ssh

import io.mainframe.hacs.main.Status

/**
 * Created by holger on 11.08.16.
 */
class DoorCommand private constructor(private val command: String) {
    fun get(): String {
        return this.command
    }

    override fun toString(): String {
        return "DoorCommand(command='$command')"
    }


    companion object {
        fun getSwitchDoorStateCmd(status: Status): DoorCommand {
            return DoorCommand("set-status " + status.mqttValue)
        }

        @JvmField
        val innerGlassDoorBuzzerCmd = DoorCommand("open-door glass")

        @JvmField
        val innerMetalDoorBuzzerCmd = DoorCommand("open-door main")

        @JvmField
        val outerDoorBuzzerCmd = DoorCommand("open-door downstairs")
    }
}
