package com.support.tickets.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TicketCategory {
    ACCOUNT_ACCESS("account_access"),
    TECHNICAL_ISSUE("technical_issue"),
    BILLING_QUESTION("billing_question"),
    FEATURE_REQUEST("feature_request"),
    BUG_REPORT("bug_report"),
    OTHER("other");

    private final String value;

    TicketCategory(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static TicketCategory fromValue(String value) {
        for (TicketCategory c : values()) {
            if (c.value.equalsIgnoreCase(value)) return c;
        }
        throw new IllegalArgumentException("Unknown category: " + value);
    }
}
