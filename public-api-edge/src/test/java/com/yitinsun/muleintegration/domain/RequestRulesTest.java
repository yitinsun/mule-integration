package com.yitinsun.muleintegration.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RequestRulesTest {

    @Test
    void acceptsAgifyName() {
        assertEquals("michael", RequestRules.name("michael"));
    }

    @Test
    void rejectsBlankName() {
        assertEquals(RequestRules.NAME_REQUIRED, assertThrows(IllegalArgumentException.class, () -> RequestRules.name(null)).getMessage());
        assertEquals(RequestRules.NAME_REQUIRED, assertThrows(IllegalArgumentException.class, () -> RequestRules.name("  ")).getMessage());
    }

    @Test
    void rejectsInvalidName() {
        assertEquals(RequestRules.NAME_INVALID, assertThrows(IllegalArgumentException.class, () -> RequestRules.name("a".repeat(65))).getMessage());
        assertEquals(RequestRules.NAME_INVALID, assertThrows(IllegalArgumentException.class, () -> RequestRules.name("1michael")).getMessage());
    }

    @Test
    void formatsJsonTextOrRejects() {
        assertEquals("json", RequestRules.format(null));
        assertEquals("text", RequestRules.format("text"));
        assertEquals(RequestRules.FORMAT_INVALID, assertThrows(IllegalArgumentException.class, () -> RequestRules.format("xml")).getMessage());
    }

    @Test
    void normalizesCountryAndPostalCode() {
        assertEquals("us", RequestRules.country("US"));
        assertEquals(RequestRules.POSTAL_INVALID, assertThrows(IllegalArgumentException.class, () -> RequestRules.country("USA")).getMessage());
        assertEquals("90210", RequestRules.postalCode("90210"));
        assertEquals("A0A", RequestRules.postalCode("A0A"));
        assertEquals("100 00", RequestRules.postalCode("100 00"));
        assertEquals(RequestRules.POSTAL_INVALID, assertThrows(IllegalArgumentException.class, () -> RequestRules.postalCode("../etc")).getMessage());
        assertEquals(RequestRules.POSTAL_INVALID, assertThrows(IllegalArgumentException.class, () -> RequestRules.postalCode("90/210")).getMessage());
    }

    @Test
    void parsesCoinIds() {
        assertEquals(List.of(1L, 1027L), RequestRules.coinIds("1,1027"));
        assertEquals(RequestRules.IDS_INVALID, assertThrows(IllegalArgumentException.class, () -> RequestRules.coinIds("BTC")).getMessage());
        assertEquals(RequestRules.IDS_INVALID, assertThrows(IllegalArgumentException.class, () -> RequestRules.coinIds("1, 2")).getMessage());
        String tooMany = String.join(",", Collections.nCopies(51, "1"));
        assertEquals(RequestRules.IDS_INVALID, assertThrows(IllegalArgumentException.class, () -> RequestRules.coinIds(tooMany)).getMessage());
    }

    @Test
    void readsFlags() {
        assertFalse(RequestRules.flag(null));
        assertTrue(RequestRules.flag("true"));
        assertEquals(RequestRules.FLAG_INVALID, assertThrows(IllegalArgumentException.class, () -> RequestRules.flag("yes")).getMessage());
    }

    @Test
    void resolvesProviderAndOperation() {
        assertEquals(RequestRules.INTEGRATION_INVALID, assertThrows(IllegalArgumentException.class, () -> RequestRules.provider("Agify")).getMessage());
        assertEquals(Provider.IPIFY, RequestRules.provider("ipify"));
        assertEquals("predict-age", RequestRules.operation(Provider.AGIFY, null));
        assertEquals(RequestRules.INTEGRATION_INVALID, assertThrows(IllegalArgumentException.class, () -> RequestRules.operation(Provider.AGIFY, "echo-get")).getMessage());
    }

    @Test
    void rejectsUnknownParamsAndQueryKeys() {
        assertEquals(
                RequestRules.INTEGRATION_INVALID,
                assertThrows(IllegalArgumentException.class, () -> RequestRules.params(Provider.IPIFY, Map.of("url", "http://evil"))).getMessage());
        assertEquals(
                RequestRules.UNKNOWN_QUERY,
                assertThrows(IllegalArgumentException.class, () -> RequestRules.rejectUnknownQuery(Map.of("extra", new String[] {"1"}), Set.of("format"))).getMessage());
    }

    @Test
    void acceptsParamsForEachProvider() {
        assertEquals("text", RequestRules.params(Provider.IPIFY, Map.of("format", "text")).get("format"));
        assertEquals("michael", RequestRules.params(Provider.AGIFY, Map.of("name", "michael")).get("name"));
        assertTrue(RequestRules.params(Provider.HTTPBIN, Map.of()).isEmpty());
        Map<String, Object> postal = RequestRules.params(Provider.ZIPPOPOTAM, Map.of("country", "US", "postalCode", "90210"));
        assertEquals("us", postal.get("country"));
        assertEquals("90210", postal.get("postalCode"));
        Map<String, Object> price = RequestRules.params(Provider.COINMARKETCAP, Map.of("ids", "1", "includeMarketCap", true));
        assertEquals("1", price.get("ids"));
        assertEquals(true, price.get("includeMarketCap"));
        assertEquals(false, price.get("includeVolume24h"));
    }

    @Test
    void rejectsMissingPostalParamAndNestedValues() {
        assertEquals(
                RequestRules.INTEGRATION_INVALID,
                assertThrows(IllegalArgumentException.class, () -> RequestRules.params(Provider.ZIPPOPOTAM, Map.of("country", "us"))).getMessage());
        assertEquals(
                RequestRules.INTEGRATION_INVALID,
                assertThrows(IllegalArgumentException.class, () -> RequestRules.params(Provider.AGIFY, Map.of("name", Map.of("nested", "x")))).getMessage());
    }

    @Test
    void phrasesMatchTheErrorContract() {
        assertEquals("Bad Request", RequestRules.phrase(400));
        assertEquals("Not Found", RequestRules.phrase(404));
        assertEquals("Unsupported Media Type", RequestRules.phrase(415));
        assertEquals("Too Many Requests", RequestRules.phrase(429));
        assertEquals("Bad Gateway", RequestRules.phrase(502));
        assertEquals("Service Unavailable", RequestRules.phrase(503));
        assertEquals("Gateway Timeout", RequestRules.phrase(504));
    }
}
