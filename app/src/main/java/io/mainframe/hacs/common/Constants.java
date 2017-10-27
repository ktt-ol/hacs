package io.mainframe.hacs.common;

/**
 * Created by holger on 20.11.15.
 */
public final class Constants {

    /**
     * Our mainframe SSID's
     */
    public static final String[] MAINFRAME_SSIDS = new String[]{"mainframe", "mainframe-legacy"};

    public static final String DOOR_SERVER_HOST = "192.168.2.3";
    public static final int DOOR_SERVER_PORT = 22;
    public static final String DOOR_SERVER_HOST_KEY = "C1:28:56:42:2B:8D:45:30:B7:43:EB:F6:A7:36:43:5D";

    public static final String KEYSTORE_FILE = "keystore/hacs_keystore.bks";
    public static final String KEYSTORE_PW = "keystorepw";
    public static final String MQTT_USER = "hacs_app";
    public static final String MQTT_SERVER = "ssl://mainframe.io:8883";
    public static final String MQTT_TOPIC_STATUS = "/access-control-system/space-state";
    public static final String MQTT_TOPIC_STATUS_NEXT = "/access-control-system/space-state-next";
    public static final String MQTT_TOPIC_KEYHOLDER = "/access-control-system/keyholder/name";
    public static final String MQTT_TOPIC_DEVICES = "/net/devices";

    public static final String DOOR_USER = "keyholder";
}
