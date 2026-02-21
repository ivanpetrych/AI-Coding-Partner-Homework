package com.support.tickets.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MetadataSource {
    WEB_FORM("web_form"),
    EMAIL("email"),
    API("api"),
    CHAT("chat"),
    PHONE("phone");

    private final String value;

    MetadataSource(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static MetadataSource fromValue(String value) {
        if (value == null) return null;
        for (MetadataSource s : values()) {
            if (s.value.equalsIgnoreCase(value)) return s;
        }
        throw new IllegalArgumentException("Unknown source: " + value);
    }
}
