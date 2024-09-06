package io.mainframe.hacs.status

enum class StatusEvent(val eventName: String) {
    SPACE_STATUS("spaceOpen"),
    KEYHOLDER("keyholder"),
    DEVICES("spaceDevices"),
    STATUS_MACHINING("machining");

    companion object {
        fun forEventNameOrNull(value: String): StatusEvent? {
            entries.forEach {
                if (value == it.eventName) {
                    return it
                }
            }
            return null
        }
    }
}