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
            "acs.lan.mainframe.io", "keyholder", 22,
            "C1:28:56:42:2B:8D:45:30:B7:43:EB:F6:A7:36:43:5D");

    public static final DoorServer MACHINING_DOOR = new DoorServer(
            "acs-machining.lan.mainframe.io", "keyholder", 22,
            "B9:24:BC:26:8B:27:CE:0A:B5:8A:4E:BA:F4:CD:0C:84");

    public static final String KEYSTORE_FILE = "keystore/hacs_keystore.bks";
    public static final String KEYSTORE_PW = "keystorepw";

    public static final String STATUS_SSE_URL = "https://status.mainframe.io/api/statusStream?spaceOpen=1&machining=1&spaceDevices=1&mqtt=1&keyholder=1";
    public static final String LOG_FILE_FOLDER = "hacs_logs";

    public static final class DoorServer {
        public final String host;
        public final String user;
        public final int port;
        public final String hostKey;

        public DoorServer(String host, String user, int port, String hostKey) {
            this.host = host;
            this.user = user;
            this.port = port;
            this.hostKey = hostKey;
        }
    }
}
