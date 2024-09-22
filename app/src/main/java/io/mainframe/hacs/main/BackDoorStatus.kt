package io.mainframe.hacs.main

enum class BackDoorStatus(val mqttValue: String) {
    UNKNOWN("-1"),
    OPEN("0"),
    CLOSED("1");

    companion object {
        fun byMqttValue(value: String): BackDoorStatus {
            for (status in entries) {
                if (status.mqttValue == value) {
                    return status
                }
            }

            throw IllegalArgumentException("Unsupported mqtt value: $value")
        }
    }
}
