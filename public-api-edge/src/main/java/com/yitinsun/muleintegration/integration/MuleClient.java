package com.yitinsun.muleintegration.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yitinsun.muleintegration.domain.RequestRules;
import com.yitinsun.muleintegration.web.dto.AgeResponse;
import com.yitinsun.muleintegration.web.dto.CryptoPriceResponse;
import com.yitinsun.muleintegration.web.dto.EchoResponse;
import com.yitinsun.muleintegration.web.dto.ErrorResponse;
import com.yitinsun.muleintegration.web.dto.IntegrateResponse;
import com.yitinsun.muleintegration.web.dto.IpResponse;
import com.yitinsun.muleintegration.web.dto.PostalResponse;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class MuleClient {

    private static final Set<String> KNOWN_MESSAGES = Set.of(
            RequestRules.NAME_REQUIRED,
            RequestRules.NAME_INVALID,
            RequestRules.FORMAT_INVALID,
            RequestRules.POSTAL_INVALID,
            RequestRules.IDS_INVALID,
            RequestRules.FLAG_INVALID,
            RequestRules.UNKNOWN_QUERY,
            RequestRules.INTEGRATION_INVALID,
            RequestRules.CONTENT_TYPE,
            RequestRules.NOT_FOUND_POSTAL,
            RequestRules.UPSTREAM_NOT_FOUND,
            RequestRules.RATE_LIMIT,
            RequestRules.UNUSABLE,
            RequestRules.UNAVAILABLE,
            RequestRules.TIMEOUT);

    private final RestClient.Builder builder;
    private final ObjectMapper objectMapper;

    public MuleClient(RestClient.Builder builder, ObjectMapper objectMapper) {
        this.builder = builder;
        this.objectMapper = objectMapper;
    }

    public IpResponse publicIp(String format) {
        IpResponse body = call("ipify", IpResponse.class, spec -> spec.get()
                .uri(uri -> uri.path("/providers/ipify").queryParam("format", format).build()));
        if (body.ip() == null || body.ip().isBlank()) {
            throw new UpstreamException(502, RequestRules.UNUSABLE, "ipify");
        }
        return body;
    }

    public AgeResponse age(String name) {
        return call("agify", AgeResponse.class, spec -> spec.get()
                .uri(uri -> uri.path("/providers/agify").queryParam("name", name).build()));
    }

    public EchoResponse echo() {
        return call("httpbin", EchoResponse.class, spec -> spec.get().uri("/providers/httpbin"));
    }

    public PostalResponse postal(String country, String postalCode) {
        return call("zippopotam", PostalResponse.class, spec -> spec.get()
                .uri("/providers/zippopotam/{country}/{postalCode}", country, postalCode));
    }

    public CryptoPriceResponse price(
            String ids,
            boolean includeMarketCap,
            boolean includeVolume24h,
            boolean includePercentChange24h,
            boolean includeLastUpdated) {
        return call("coinmarketcap", CryptoPriceResponse.class, spec -> spec.get()
                .uri(uri -> {
                    uri.path("/providers/coinmarketcap/price").queryParam("ids", ids);
                    if (includeMarketCap) {
                        uri.queryParam("includeMarketCap", "true");
                    }
                    if (includeVolume24h) {
                        uri.queryParam("includeVolume24h", "true");
                    }
                    if (includePercentChange24h) {
                        uri.queryParam("includePercentChange24h", "true");
                    }
                    if (includeLastUpdated) {
                        uri.queryParam("includeLastUpdated", "true");
                    }
                    return uri.build();
                }));
    }

    public IntegrateResponse integrate(String provider, String operation, Map<String, Object> params) {
        Map<String, Object> body = Map.of(
                "provider", provider,
                "operation", operation,
                "params", params);
        return call(provider, IntegrateResponse.class, spec -> spec.post()
                .uri("/integrate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body));
    }

    private <T> T call(String provider, Class<T> type, RequestCustomizer customizer) {
        try {
            T body = customizer.apply(http())
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (request, response) -> {
                        String raw = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        throw toUpstream(response.getStatusCode().value(), raw, provider);
                    })
                    .body(type);
            if (body == null) {
                throw new UpstreamException(502, RequestRules.UNUSABLE, provider);
            }
            return body;
        } catch (UpstreamException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            throw mapAccess(ex, provider);
        } catch (RestClientException ex) {
            throw unwrap(ex, provider);
        }
    }

    private RestClient http() {
        return builder.build();
    }

    private RuntimeException unwrap(RestClientException ex, String provider) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof UpstreamException upstream) {
                return upstream;
            }
            if (current instanceof ResourceAccessException access) {
                return mapAccess(access, provider);
            }
            current = current.getCause();
        }
        return new UpstreamException(502, RequestRules.UNUSABLE, provider);
    }

    private UpstreamException mapAccess(ResourceAccessException ex, String provider) {
        if (isTimeout(ex)) {
            return new UpstreamException(504, RequestRules.TIMEOUT, provider);
        }
        return new UpstreamException(503, RequestRules.UNAVAILABLE, provider);
    }

    private static boolean isTimeout(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("timed out")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private UpstreamException toUpstream(int status, String raw, String provider) {
        if (status == 504) {
            return new UpstreamException(504, knownOr(raw, RequestRules.TIMEOUT), provider);
        }
        if (status == 400 || status == 404 || status == 415 || status == 429 || status == 502) {
            return new UpstreamException(status, knownOr(raw, defaultMessage(status)), provider);
        }
        return new UpstreamException(502, RequestRules.UNUSABLE, provider);
    }

    private String knownOr(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            ErrorResponse parsed = objectMapper.readValue(raw, ErrorResponse.class);
            if (parsed.message() != null && KNOWN_MESSAGES.contains(parsed.message())) {
                return parsed.message();
            }
        } catch (Exception ignored) {
            return fallback;
        }
        return fallback;
    }

    private static String defaultMessage(int status) {
        return switch (status) {
            case 400 -> RequestRules.INTEGRATION_INVALID;
            case 404 -> RequestRules.UPSTREAM_NOT_FOUND;
            case 415 -> RequestRules.CONTENT_TYPE;
            case 429 -> RequestRules.RATE_LIMIT;
            case 503 -> RequestRules.UNAVAILABLE;
            case 504 -> RequestRules.TIMEOUT;
            default -> RequestRules.UNUSABLE;
        };
    }

    @FunctionalInterface
    private interface RequestCustomizer {
        RestClient.RequestHeadersSpec<?> apply(RestClient client);
    }
}
