package io.mainframe.hacs.main;

public enum BackDoorStatus {

    UNKNOWN("-1"),
    OPEN("0"),
    CLOSED("1");

    private final String mqttValue;

    BackDoorStatus(String mqttValue) {
        this.mqttValue = mqttValue;
    }

    public static BackDoorStatus byMqttValue(String value) {
        for (BackDoorStatus status : values()) {
            if (status.getMqttValue().equals(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unsupported mqtt value: " + value);
    }

    public String getMqttValue() {
        return this.mqttValue;
    }
}
