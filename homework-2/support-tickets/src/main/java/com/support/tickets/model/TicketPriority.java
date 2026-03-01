package com.support.tickets.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TicketPriority {
    URGENT("urgent"),
    HIGH("high"),
    MEDIUM("medium"),
    LOW("low");

    private final String value;

    TicketPriority(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static TicketPriority fromValue(String value) {
        for (TicketPriority p : values()) {
            if (p.value.equalsIgnoreCase(value)) return p;
        }
        throw new IllegalArgumentException("Unknown priority: " + value);
    }
}
