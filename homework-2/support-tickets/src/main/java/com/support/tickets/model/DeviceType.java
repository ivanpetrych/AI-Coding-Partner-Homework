package com.support.tickets.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DeviceType {
    DESKTOP("desktop"),
    MOBILE("mobile"),
    TABLET("tablet");

    private final String value;

    DeviceType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static DeviceType fromValue(String value) {
        if (value == null) return null;
        for (DeviceType d : values()) {
            if (d.value.equalsIgnoreCase(value)) return d;
        }
        throw new IllegalArgumentException("Unknown device type: " + value);
    }
}
