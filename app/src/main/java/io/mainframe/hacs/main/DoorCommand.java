package io.mainframe.hacs.main;

/**
 * Created by holger on 11.08.16.
 */
public final class DoorCommand {

    private static final String CMD = "set-status";

    public static String getSwitchDoorStateCmd(Status status) {
        return CMD + " " + status.getValue();
    }

    public static String getDoorBuzzerCmd() {
        return "open-door main";
    }
}
