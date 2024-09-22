package io.mainframe.hacs.main

import org.json.JSONException
import org.json.JSONObject

/**
 * Created by holger on 23.08.16.
 */
enum class Status(
    @JvmField val mqttValue: String,
    val uiValue: String = mqttValue
) {
    NOT_SET("", "not-set"),
    CLOSE("none", "closed"),
    CLOSING("closing"),
    KEYHOLDER("keyholder"),
    MEMBER("member"),
    OPEN("open"),
    OPEN_PLUS("open+");

    companion object {
        fun byEventStatusValue(jsonValue: String): Status {
            try {
                val json = JSONObject(jsonValue)
                val value = json.getString("state")
                for (status in entries) {
                    if (status.mqttValue == value) {
                        return status
                    }
                }

                throw IllegalArgumentException("Unsupported mqttValue: $value")
            } catch (e: JSONException) {
                throw IllegalArgumentException("Unexpected json for Status: $jsonValue")
            }
        }
    }
}
