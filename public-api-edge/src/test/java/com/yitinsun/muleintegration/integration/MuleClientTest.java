package com.yitinsun.muleintegration.integration;

import static com.yitinsun.muleintegration.support.FixtureLoader.fixture;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.yitinsun.muleintegration.domain.RequestRules;
import com.yitinsun.muleintegration.web.dto.AgeResponse;
import com.yitinsun.muleintegration.web.dto.CryptoPriceResponse;
import com.yitinsun.muleintegration.web.dto.IntegrateResponse;
import com.yitinsun.muleintegration.web.dto.IpResponse;
import com.yitinsun.muleintegration.web.dto.PostalResponse;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class MuleClientTest {

    @Autowired
    private RestClient.Builder restClientBuilder;

    @Autowired
    private MuleClient client;

    private MockRestServiceServer server;

    @BeforeEach
    void bindMule() {
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
    }

    @Test
    void requestsJsonIp() throws IOException {
        server.expect(requestTo(containsString("/providers/ipify")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("format", "json"))
                .andRespond(withSuccess(fixture("ipify.json"), MediaType.APPLICATION_JSON));

        IpResponse body = client.publicIp("json");

        assertEquals("203.0.113.10", body.ip());
        server.verify();
    }

    @Test
    void requestsTextFormatAndStillReadsJson() throws IOException {
        server.expect(requestTo(containsString("/providers/ipify")))
                .andExpect(queryParam("format", "text"))
                .andRespond(withSuccess(fixture("ipify.json"), MediaType.APPLICATION_JSON));

        assertEquals("203.0.113.10", client.publicIp("text").ip());
        server.verify();
    }

    @Test
    void encodesAgeName() throws IOException {
        server.expect(request -> {
                    String raw = request.getURI().getRawQuery();
                    assertTrue(raw != null && (raw.contains("mary%20anne") || raw.contains("mary+anne")), raw);
                })
                .andRespond(withSuccess(fixture("agify.json"), MediaType.APPLICATION_JSON));

        AgeResponse body = client.age("mary anne");

        assertEquals("michael", body.name());
        server.verify();
    }

    @Test
    void requestsPostalPath() throws IOException {
        server.expect(requestTo(containsString("/providers/zippopotam/us/90210")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(fixture("zippopotam-90210.json"), MediaType.APPLICATION_JSON));

        PostalResponse body = client.postal("us", "90210");

        assertEquals("Beverly Hills", body.places().get(0).placeName());
        server.verify();
    }

    @Test
    void requestsPriceFlags() throws IOException {
        server.expect(requestTo(containsString("/providers/coinmarketcap/price")))
                .andExpect(queryParam("ids", "1"))
                .andExpect(queryParam("includeMarketCap", "true"))
                .andRespond(withSuccess(fixture("cmc-price.json"), MediaType.APPLICATION_JSON));

        CryptoPriceResponse body = client.price("1", true, false, false, false);

        assertEquals("USD", body.currency());
        assertEquals(1L, body.prices().get(0).id());
        server.verify();
    }

    @Test
    void postsIntegrateBody() throws IOException {
        server.expect(requestTo(containsString("/integrate")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"provider":"agify","operation":"predict-age","params":{"name":"michael"}}
                        """))
                .andRespond(withSuccess("""
                        {"provider":"agify","operation":"predict-age","data":{"name":"michael","age":69,"count":298219}}
                        """, MediaType.APPLICATION_JSON));

        IntegrateResponse body = client.integrate("agify", "predict-age", Map.of("name", "michael"));

        assertEquals("agify", body.provider());
        assertEquals("predict-age", body.operation());
        assertEquals("michael", body.data().get("name").asText());
        server.verify();
    }

    @Test
    void mapsConnectionFailure() {
        server.expect(requestTo(containsString("/providers/ipify")))
                .andRespond(request -> {
                    throw new ResourceAccessException("connect failed", new ConnectException("Connection refused"));
                });

        UpstreamException ex = assertThrows(UpstreamException.class, () -> client.publicIp("json"));

        assertEquals(503, ex.status());
        assertEquals(RequestRules.UNAVAILABLE, ex.getMessage());
        server.verify();
    }

    @Test
    void mapsPostalNotFound() {
        server.expect(requestTo(containsString("/providers/zippopotam/us/00000")))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"status":404,"error":"Not Found","message":"Postal code was not found.","provider":"zippopotam"}
                                """));

        UpstreamException ex = assertThrows(UpstreamException.class, () -> client.postal("us", "00000"));

        assertEquals(404, ex.status());
        assertEquals(RequestRules.NOT_FOUND_POSTAL, ex.getMessage());
        server.verify();
    }

    @Test
    void mapsGatewayTimeout() {
        server.expect(requestTo(containsString("/providers/httpbin")))
                .andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"status":504,"error":"Gateway Timeout","message":"Upstream request timed out.","provider":"httpbin"}
                                """));

        UpstreamException ex = assertThrows(UpstreamException.class, () -> client.echo());

        assertEquals(504, ex.status());
        assertEquals(RequestRules.TIMEOUT, ex.getMessage());
        server.verify();
    }

    @Test
    void mapsUnreadableSuccessBody() {
        server.expect(requestTo(containsString("/providers/ipify")))
                .andRespond(withSuccess("{", MediaType.APPLICATION_JSON));

        UpstreamException ex = assertThrows(UpstreamException.class, () -> client.publicIp("json"));

        assertEquals(502, ex.status());
        assertEquals(RequestRules.UNUSABLE, ex.getMessage());
        server.verify();
    }

    @Test
    void mapsUpstreamServerError() {
        server.expect(requestTo(containsString("/providers/httpbin")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("nope"));

        UpstreamException ex = assertThrows(UpstreamException.class, () -> client.echo());

        assertEquals(502, ex.status());
        assertEquals(RequestRules.UNUSABLE, ex.getMessage());
        server.verify();
    }

    @Test
    void mapsRateLimit() {
        server.expect(requestTo(containsString("/providers/ipify")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"status":429,"error":"Too Many Requests","message":"Upstream rate limit was reached.","provider":"ipify"}
                                """));

        UpstreamException ex = assertThrows(UpstreamException.class, () -> client.publicIp("json"));

        assertEquals(429, ex.status());
        assertEquals(RequestRules.RATE_LIMIT, ex.getMessage());
        server.verify();
    }
}
