package com.serotonin.bacnet4j.util.sero;

public class HostNotConfiguredException extends IllegalArgumentException{
    public HostNotConfiguredException(String host) {
        super("Host '" + host + "' is not configured on any network interface.");
    }
}
