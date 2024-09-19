package io.mainframe.hacs.main;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by holger on 23.08.16.
 */
public enum Status {

    NOT_SET("", "not-set"),
    CLOSE("none", "closed"),
    CLOSING("closing"),
    KEYHOLDER("keyholder"),
    MEMBER("member"),
    OPEN("open"),
    OPEN_PLUS("open+");

    private String mqttValue;
    private String uiValue;

    Status(String mqttValue) {
        this(mqttValue, mqttValue);
    }

    Status(String mqttValue, String uiValue) {
        this.mqttValue = mqttValue;
        this.uiValue = uiValue;
    }

    public static Status byEventStatusValue(String jsonValue) {
        try {
            JSONObject json = new JSONObject(jsonValue);
            String value = json.getString("state");
            for (Status status : values()) {
                if (status.getMqttValue().equals(value)) {
                    return status;
                }
            }

            throw new IllegalArgumentException("Unsupported mqttValue: " + value);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Unexpected json for Status: " + jsonValue);
        }
    }

    public String getMqttValue() {
        return this.mqttValue;
    }

    public String getUiValue() {
        return uiValue;
    }
}
