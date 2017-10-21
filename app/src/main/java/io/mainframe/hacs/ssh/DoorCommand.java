package io.mainframe.hacs.ssh;

import io.mainframe.hacs.main.Status;

/**
 * Created by holger on 11.08.16.
 */
public final class DoorCommand {

    private static final String CMD = "set-status";

    public static String getSwitchDoorStateCmd(Status status) {
        return CMD + " " + status.getMqttValue();
    }

    public static String getDoorBuzzerCmd() {
        return "open-door main";
    }
}
