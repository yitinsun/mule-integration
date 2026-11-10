package com.yitinsun.muleintegration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mule")
public record MuleProperties(String baseUrl, int connectTimeoutMs, int readTimeoutMs) {
}
