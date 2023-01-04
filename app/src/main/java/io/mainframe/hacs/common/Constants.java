package io.mainframe.hacs.common;

/**
 * Created by holger on 20.11.15.
 */
public final class Constants {

    /**
     * Our mainframe SSIDs
     */
    public static final String[] MAINFRAME_SSIDS = new String[]{"mainframe", "mainframe-legacy"};

    /**
     * Valid bssids for the machining page
     */
    public static final String[] MACHINING_WIFI_BSSIDS = new String[]{
            // AP 111 - Fräsraum
            "80:2a:a8:47:64:2d", // mainframe-legacy
            "80:2a:a8:48:64:2d", // mainframe
    };

    public static final DoorServer SPACE_DOOR = new DoorServer(
            "192.168.2.3", "keyholder", 22,
            "C1:28:56:42:2B:8D:45:30:B7:43:EB:F6:A7:36:43:5D");

    public static final DoorServer MACHINING_DOOR = new DoorServer(
            "acs-machining.lan.mainframe.io", "keyholder", 22,
            "B9:24:BC:26:8B:27:CE:0A:B5:8A:4E:BA:F4:CD:0C:84");

    public static final String KEYSTORE_FILE = "keystore/hacs_keystore.bks";
    public static final String KEYSTORE_PW = "keystorepw";

    public static final String MQTT_USER = "hacs_app";
    public static final String MQTT_SERVER = "ssl://mainframe.io:8883";
    public static final String MQTT_TOPIC_STATUS = "/access-control-system/space-state";
    public static final String MQTT_TOPIC_STATUS_NEXT = "/access-control-system/space-state-next";
    public static final String MQTT_TOPIC_KEYHOLDER = "/access-control-system/keyholder/name";
    public static final String MQTT_TOPIC_DEVICES = "/net/devices";
    public static final String MQTT_TOPIC_MACHINING_STATUS = "/access-control-system/machining/state";
    public static final String MQTT_TOPIC_MACHINING_KEYHOLDER = "/access-control-system/machining/keyholder/name";
    public static final String MQTT_TOPIC_BACK_DOOR_BOLT = "/access-control-system/back-door/bolt-contact";

    public static final String LOG_FILE_FOLDER = "hacs_logs";

    public static final class DoorServer {
        public final String host;
        public final String user;
        public final int port;
        public final String hostKey;

        DoorServer(String host, String user, int port, String hostKey) {
            this.host = host;
            this.user = user;
            this.port = port;
            this.hostKey = hostKey;
        }
    }
}
