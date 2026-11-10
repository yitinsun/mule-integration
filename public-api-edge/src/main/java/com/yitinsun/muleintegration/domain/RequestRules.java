package com.yitinsun.muleintegration.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class RequestRules {

    public static final String NAME_REQUIRED = "Query parameter name is required.";
    public static final String NAME_INVALID = "Query parameter name is invalid.";
    public static final String FORMAT_INVALID = "Query parameter format must be json or text.";
    public static final String POSTAL_INVALID = "Postal path is invalid.";
    public static final String IDS_INVALID = "Query parameter ids must be 1 to 50 numeric CoinMarketCap ids.";
    public static final String FLAG_INVALID = "Include flag must be true or false.";
    public static final String UNKNOWN_QUERY = "Unknown query parameter.";
    public static final String INTEGRATION_INVALID = "Integration request is invalid.";
    public static final String CONTENT_TYPE = "Content-Type must be application/json.";
    public static final String NOT_FOUND_POSTAL = "Postal code was not found.";
    public static final String UPSTREAM_NOT_FOUND = "Upstream resource was not found.";
    public static final String RATE_LIMIT = "Upstream rate limit was reached.";
    public static final String UNUSABLE = "Upstream response was not usable.";
    public static final String UNAVAILABLE = "Integration layer is unavailable.";
    public static final String TIMEOUT = "Upstream request timed out.";
    public static final String PROVIDER_ATTR = "integration.provider";

    private static final Pattern NAME = Pattern.compile("^[\\p{L}][\\p{L} .'\\-]{0,63}$");
    private static final Pattern COUNTRY = Pattern.compile("[A-Za-z]{2}");
    private static final Pattern POSTAL = Pattern.compile("[A-Za-z0-9][A-Za-z0-9 -]{0,19}");
    private static final Pattern COIN_ID = Pattern.compile("\\d{1,10}");

    private RequestRules() {
    }

    public static String format(String raw) {
        if (raw == null) {
            return "json";
        }
        if ("json".equals(raw) || "text".equals(raw)) {
            return raw;
        }
        throw new IllegalArgumentException(FORMAT_INVALID);
    }

    public static String name(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(NAME_REQUIRED);
        }
        String trimmed = raw.trim();
        if (!NAME.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(NAME_INVALID);
        }
        return trimmed;
    }

    public static String country(String raw) {
        if (raw == null || !COUNTRY.matcher(raw).matches()) {
            throw new IllegalArgumentException(POSTAL_INVALID);
        }
        return raw.toLowerCase(Locale.ROOT);
    }

    public static String postalCode(String raw) {
        if (raw == null || !POSTAL.matcher(raw).matches()) {
            throw new IllegalArgumentException(POSTAL_INVALID);
        }
        return raw;
    }

    public static List<Long> coinIds(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(IDS_INVALID);
        }
        String[] parts = raw.split(",", -1);
        if (parts.length < 1 || parts.length > 50) {
            throw new IllegalArgumentException(IDS_INVALID);
        }
        java.util.ArrayList<Long> ids = new java.util.ArrayList<>();
        for (String part : parts) {
            if (!COIN_ID.matcher(part).matches()) {
                throw new IllegalArgumentException(IDS_INVALID);
            }
            ids.add(Long.parseLong(part));
        }
        return List.copyOf(ids);
    }

    public static boolean flag(String raw) {
        if (raw == null) {
            return false;
        }
        if ("true".equals(raw)) {
            return true;
        }
        if ("false".equals(raw)) {
            return false;
        }
        throw new IllegalArgumentException(FLAG_INVALID);
    }

    public static Provider provider(String raw) {
        if (raw != null) {
            for (Provider candidate : Provider.values()) {
                if (candidate.id().equals(raw)) {
                    return candidate;
                }
            }
        }
        throw new IllegalArgumentException(INTEGRATION_INVALID);
    }

    public static String operation(Provider provider, String raw) {
        if (raw == null) {
            return provider.operation();
        }
        if (!provider.operation().equals(raw)) {
            throw new IllegalArgumentException(INTEGRATION_INVALID);
        }
        return raw;
    }

    public static Map<String, Object> params(Provider provider, Map<String, Object> raw) {
        Map<String, Object> source = raw == null ? Map.of() : raw;
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (!provider.paramKeys().contains(entry.getKey())) {
                throw new IllegalArgumentException(INTEGRATION_INVALID);
            }
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> || value instanceof List<?>) {
                throw new IllegalArgumentException(INTEGRATION_INVALID);
            }
        }
        return switch (provider) {
            case IPIFY -> ipifyParams(source);
            case AGIFY -> Map.of("name", requiredString(source, "name", () -> name(stringValue(source.get("name")))));
            case HTTPBIN -> Map.of();
            case ZIPPOPOTAM -> zipParams(source);
            case COINMARKETCAP -> cmcParams(source);
        };
    }

    public static void rejectUnknownQuery(Map<String, ?> query, java.util.Set<String> allowed) {
        if (query == null) {
            return;
        }
        for (String key : query.keySet()) {
            if (!allowed.contains(key)) {
                throw new IllegalArgumentException(UNKNOWN_QUERY);
            }
        }
    }

    public static String phrase(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 415 -> "Unsupported Media Type";
            case 429 -> "Too Many Requests";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            default -> "Bad Gateway";
        };
    }

    private static Map<String, Object> ipifyParams(Map<String, Object> source) {
        Object raw = source.get("format");
        if (raw == null) {
            return Map.of("format", "json");
        }
        if (!(raw instanceof String text)) {
            throw new IllegalArgumentException(INTEGRATION_INVALID);
        }
        return Map.of("format", format(text));
    }

    private static Map<String, Object> zipParams(Map<String, Object> source) {
        if (!source.containsKey("country") || !source.containsKey("postalCode")
                || source.get("country") == null || source.get("postalCode") == null) {
            throw new IllegalArgumentException(INTEGRATION_INVALID);
        }
        if (!(source.get("country") instanceof String country) || !(source.get("postalCode") instanceof String postal)) {
            throw new IllegalArgumentException(INTEGRATION_INVALID);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("country", country(country));
        out.put("postalCode", postalCode(postal));
        return Map.copyOf(out);
    }

    private static Map<String, Object> cmcParams(Map<String, Object> source) {
        if (!(source.get("ids") instanceof String ids)) {
            throw new IllegalArgumentException(INTEGRATION_INVALID);
        }
        coinIds(ids);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ids", ids);
        out.put("includeMarketCap", flagValue(source.get("includeMarketCap")));
        out.put("includeVolume24h", flagValue(source.get("includeVolume24h")));
        out.put("includePercentChange24h", flagValue(source.get("includePercentChange24h")));
        out.put("includeLastUpdated", flagValue(source.get("includeLastUpdated")));
        return Map.copyOf(out);
    }

    private static String requiredString(Map<String, Object> source, String key, java.util.function.Supplier<String> parser) {
        if (!source.containsKey(key) || source.get(key) == null) {
            throw new IllegalArgumentException(INTEGRATION_INVALID);
        }
        if (!(source.get(key) instanceof String)) {
            throw new IllegalArgumentException(INTEGRATION_INVALID);
        }
        return parser.get();
    }

    private static String stringValue(Object value) {
        return value instanceof String text ? text : null;
    }

    private static boolean flagValue(Object raw) {
        if (raw == null) {
            return false;
        }
        if (raw instanceof Boolean flag) {
            return flag;
        }
        if (raw instanceof String text) {
            return flag(text);
        }
        throw new IllegalArgumentException(INTEGRATION_INVALID);
    }
}
