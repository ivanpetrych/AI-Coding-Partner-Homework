package com.support.tickets.dto;

import com.support.tickets.model.MetadataSource;
import com.support.tickets.model.DeviceType;

public class MetadataRequest {
    private MetadataSource source;
    private String browser;
    private DeviceType deviceType;

    public MetadataSource getSource() { return source; }
    public void setSource(MetadataSource source) { this.source = source; }

    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }

    public DeviceType getDeviceType() { return deviceType; }
    public void setDeviceType(DeviceType deviceType) { this.deviceType = deviceType; }
}
