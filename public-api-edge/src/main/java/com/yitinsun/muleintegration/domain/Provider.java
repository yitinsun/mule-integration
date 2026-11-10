package com.yitinsun.muleintegration.domain;

import java.util.Set;

public enum Provider {
    IPIFY("ipify", "public-ip", Set.of("format")),
    AGIFY("agify", "predict-age", Set.of("name")),
    HTTPBIN("httpbin", "echo-get", Set.of()),
    ZIPPOPOTAM("zippopotam", "lookup-postal", Set.of()),
    COINMARKETCAP(
            "coinmarketcap",
            "simple-price",
            Set.of(
                    "ids",
                    "includeMarketCap",
                    "includeVolume24h",
                    "includePercentChange24h",
                    "includeLastUpdated"));

    private final String id;
    private final String operation;
    private final Set<String> queryKeys;

    Provider(String id, String operation, Set<String> queryKeys) {
        this.id = id;
        this.operation = operation;
        this.queryKeys = queryKeys;
    }

    public String id() {
        return id;
    }

    public String operation() {
        return operation;
    }

    public Set<String> queryKeys() {
        return queryKeys;
    }

    public Set<String> paramKeys() {
        return switch (this) {
            case IPIFY -> Set.of("format");
            case AGIFY -> Set.of("name");
            case HTTPBIN -> Set.of();
            case ZIPPOPOTAM -> Set.of("country", "postalCode");
            case COINMARKETCAP -> queryKeys;
        };
    }
}
