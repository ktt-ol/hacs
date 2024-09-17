package io.mainframe.hacs.status

enum class StatusEvent(val eventName: String) {
    SPACE_STATUS("spaceOpen"),
    KEYHOLDER("keyholder"),
    KEYHOLDER_MACHINING("keyholder_machining"),
    KEYHOLDER_WOODWORKING("keyholder_woodworking"),
    DEVICES("spaceDevices"),
    STATUS_MACHINING("machining"),
    STATUS_WOODWORKING("woodworking"),
    BACKDOOR("backdoor");

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