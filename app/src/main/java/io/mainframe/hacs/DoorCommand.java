package io.mainframe.hacs;

/**
 * Created by holger on 11.08.16.
 */
public final class DoorCommand {

    private static final String CMD = "set-status";

    public static final String CLOSE = CMD + " none";
    public static final String KEYHOLDER = CMD + " keyholder";
    public static final String MEMBER = CMD + " member";
    public static final String PUBLIC = CMD + " public";
    public static final String OPEN = CMD + " open";
}
