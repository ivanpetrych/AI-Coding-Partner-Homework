package com.support.tickets.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TicketStatus {
    NEW("new"),
    IN_PROGRESS("in_progress"),
    WAITING_CUSTOMER("waiting_customer"),
    RESOLVED("resolved"),
    CLOSED("closed");

    private final String value;

    TicketStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static TicketStatus fromValue(String value) {
        for (TicketStatus s : values()) {
            if (s.value.equalsIgnoreCase(value)) return s;
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }
}
