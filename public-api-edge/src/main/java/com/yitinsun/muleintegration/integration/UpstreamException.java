package com.yitinsun.muleintegration.integration;

public class UpstreamException extends RuntimeException {

    private final int status;
    private final String provider;

    public UpstreamException(int status, String message, String provider) {
        super(message);
        this.status = status;
        this.provider = provider;
    }

    public int status() {
        return status;
    }

    public String provider() {
        return provider;
    }
}
