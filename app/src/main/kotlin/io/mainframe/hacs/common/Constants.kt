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
    val SPACE_DOOR: DoorServer = DoorServer(
        "acs.lan.mainframe.io", "keyholder", 22,
        "C1:28:56:42:2B:8D:45:30:B7:43:EB:F6:A7:36:43:5D"
    )

    @JvmField
    val MACHINING_DOOR: DoorServer = DoorServer(
        "acs-machining.lan.mainframe.io", "keyholder", 22,
        "B9:24:BC:26:8B:27:CE:0A:B5:8A:4E:BA:F4:CD:0C:84"
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