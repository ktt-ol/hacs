package io.mainframe.hacs.ssh;

import io.mainframe.hacs.main.Status;

/**
 * Created by holger on 11.08.16.
 */
public final class DoorCommand {

    private final String command;

    private DoorCommand(String command) {
        this.command = command;
    }

    public static DoorCommand getSwitchDoorStateCmd(Status status) {
        return new DoorCommand("set-status " + status.getMqttValue());
    }

    public static DoorCommand getInnerGlassDoorBuzzerCmd() {
        return new DoorCommand("open-door glass");
    }

    public static DoorCommand getInnerMetalDoorBuzzerCmd() {
        return new DoorCommand("open-door main");
    }

    public static DoorCommand getOuterDoorBuzzerCmd() {
        return new DoorCommand("open-door downstairs");
    }

    public String get() {
        return this.command;
    }

    @Override
    public String toString() {
        return String.format("DoorCommand{command='%s'}", command);
    }
}
