package com.support.tickets.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class TicketMetadata {

    @Enumerated(EnumType.STRING)
    @Column(name = "meta_source")
    private MetadataSource source;

    @Column(name = "meta_browser")
    private String browser;

    @Enumerated(EnumType.STRING)
    @Column(name = "meta_device_type")
    private DeviceType deviceType;

    public TicketMetadata() {}

    public TicketMetadata(MetadataSource source, String browser, DeviceType deviceType) {
        this.source = source;
        this.browser = browser;
        this.deviceType = deviceType;
    }

    public MetadataSource getSource() { return source; }
    public void setSource(MetadataSource source) { this.source = source; }

    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }

    public DeviceType getDeviceType() { return deviceType; }
    public void setDeviceType(DeviceType deviceType) { this.deviceType = deviceType; }
}
