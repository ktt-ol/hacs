package io.mainframe.hacs.main;

/**
 * Created by holger on 15.12.16.
 */
public class DoorStateElement {
    // can be null for unknown
    private Status status;
    private String label;

    public DoorStateElement(Status status, String label) {
        this.status = status;
        this.label = label;
    }

    public Status getStatus() {
        return this.status;
    }

    @Override
    public String toString() {
        return this.label;
    }
}
