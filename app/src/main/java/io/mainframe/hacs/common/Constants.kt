package io.mainframe.hacs.common

/**
 * Created by holger on 20.11.15.
 */
object Constants {
    /**
     * Our mainframe SSIDs
     */
    @JvmField
    val MAINFRAME_SSIDS: Array<String> = arrayOf("mainframe", "mainframe-legacy")

    /**
     * Valid bssids for the machining page
     */
    @JvmField
    val MACHINING_WIFI_BSSIDS: Array<String> = arrayOf(
        // AP 111 - Fräsraum
        "80:2a:a8:47:64:2d",  // mainframe-legacy
        "80:2a:a8:48:64:2d",  // mainframe
    )

    @JvmField
    val WOODWORKING_FRONT_WIFI_BSSIDS = arrayOf(
        "80:2a:a8:48:65:0a", // mainframe (5 GHz)
        "80:2a:a8:47:65:0a", // mainframe-legacy (2.4 GHz)
    )

    @JvmField
    val WOODWORKING_BACK_WIFI_BSSIDS = arrayOf(
        "80:2a:a8:48:63:75",
        "80:2a:a8:48:69:d3",
        "80:2a:a8:47:63:75",
        "80:2a:a8:48:69:d3",
    )

    /*
     + How to get host key fingerprint:
     * ssh-keyscan -t ecdsa $SERVER
     * echo -n '$VAR' | base64 -d | md5sum
     */

    @JvmField
    val SPACE_DOOR_FRONT: DoorServer = DoorServer(
        "acs.lan.mainframe.io", "keyholder", 22,
        "C1:28:56:42:2B:8D:45:30:B7:43:EB:F6:A7:36:43:5D"
    )

    @JvmField
    val SPACE_DOOR_BACK: DoorServer = DoorServer(
        "acs-backdoor.lan.mainframe.io", "keyholder", 22,
        "A0:51:5F:F5:D0:4F:F8:CB:2F:D0:FC:12:44:41:3A:59"
    )



    @JvmField
    val MACHINING_DOOR: DoorServer = DoorServer(
        "acs-machining.lan.mainframe.io", "keyholder", 22,
        "B9:24:BC:26:8B:27:CE:0A:B5:8A:4E:BA:F4:CD:0C:84"
    )

    @JvmField
    val WOODWORKING_DOOR_FRONT: DoorServer = DoorServer(
        "acs-woodworking.lan.mainframe.io", "keyholder",22,
        "1D:CA:A8:86:DC:B2:A5:42:20:B7:2E:FA:2F:3C:F4:83"
    )


    @JvmField
    val WOODWORKING_DOOR_BACK: DoorServer = DoorServer(
        "acs-woodworking-backdoor.lan.mainframe.io", "keyholder",22,
        "B4:E8:4E:85:66:AE:9D:48:94:7C:DD:58:37:F4:F4:5F"
    )

    const val KEYSTORE_FILE: String = "keystore/hacs_keystore.bks"
    const val KEYSTORE_PW: String = "keystorepw"

    const val STATUS_SSE_URL: String = "https://status.mainframe.io/api/statusStream" +
            "?spaceOpen=1" +
            "&machining=1" +
            "&woodworking=1" +
            "&spaceDevices=1" +
            "&mqtt=1" +
            "&keyholder=1" +
            "&keyholder_machining=1" +
            "&keyholder_woodworking=1" +
            "&backdoor=1"

    const val LOG_FILE_FOLDER: String = "hacs_logs"

    class DoorServer(
        @JvmField val host: String,
        @JvmField val user: String,
        @JvmField val port: Int,
        @JvmField val hostKey: String
    )
}