package io.mainframe.hacs.main;

/**
 * Created by holger on 23.08.16.
 */
public enum Status {

    NOT_SET("", "not-set"),
    CLOSE("none", "closed"),
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

    public static Status byMqttValue(String value) {
        for (Status status : values()) {
            if (status.getMqttValue().equals(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unsupported mqttValue: " + value);
    }

    public String getMqttValue() {
        return this.mqttValue;
    }

    public String getUiValue() {
        return uiValue;
    }
}
