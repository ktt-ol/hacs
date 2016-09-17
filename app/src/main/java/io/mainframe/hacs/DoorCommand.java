package io.mainframe.hacs;

/**
 * Created by holger on 11.08.16.
 */
public final class DoorCommand {

    private static final String CMD = "set-status";

    public static String getCmd(Status status) {
        return CMD + " " + status.getValue();
    }
}
