package io.mainframe.hacs.common;

/**
 * Created by holger on 23.08.16.
 */
public enum Status {

    NOT_SET(""),
    CLOSE("none"),
    KEYHOLDER("keyholder"),
    MEMBER("member"),
    OPEN("open"),
    OPEN_PLUS("open+");

    private String value;

    Status(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static Status byValue(String value) {
        for (Status status : values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unsupported value: " + value);
    }
}
