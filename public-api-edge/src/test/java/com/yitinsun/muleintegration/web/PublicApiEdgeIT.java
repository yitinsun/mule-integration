package com.yitinsun.muleintegration.web;

import static com.yitinsun.muleintegration.support.FixtureLoader.fixture;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yitinsun.muleintegration.domain.RequestRules;
import java.io.IOException;
import java.net.ConnectException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicApiEdgeIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestClient.Builder restClientBuilder;

    private MockRestServiceServer server;

    @BeforeEach
    void bindMule() {
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
    }

    @Test
    void returnsPublicIpJson() throws Exception {
        stubGet("/providers/ipify", fixture("ipify.json"));

        mockMvc.perform(get("/api/v1/ip"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ip").value("203.0.113.10"));
        server.verify();
    }

    @Test
    void returnsPublicIpText() throws Exception {
        stubGet("/providers/ipify", fixture("ipify.json"));

        mockMvc.perform(get("/api/v1/ip").queryParam("format", "text"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("203.0.113.10"));
        server.verify();
    }

    @Test
    void rejectsBadIpFormatWithoutCallingMule() throws Exception {
        mockMvc.perform(get("/api/v1/ip").queryParam("format", "xml"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(RequestRules.FORMAT_INVALID))
                .andExpect(jsonPath("$.provider").value("ipify"));
        server.verify();
    }

    @Test
    void requiresAgeName() throws Exception {
        mockMvc.perform(get("/api/v1/age"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(RequestRules.NAME_REQUIRED))
                .andExpect(jsonPath("$.provider").value("agify"));
        server.verify();
    }

    @Test
    void predictsAge() throws Exception {
        stubGet("/providers/agify", fixture("agify.json"));

        mockMvc.perform(get("/api/v1/age").queryParam("name", "michael"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.age").value(69))
                .andExpect(jsonPath("$.count").value(298219));
        server.verify();
    }

    @Test
    void keepsNullAge() throws Exception {
        stubGet("/providers/agify", fixture("agify-null-age.json"));

        mockMvc.perform(get("/api/v1/age").queryParam("name", "nobody"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.age").value(nullValue()));
        server.verify();
    }

    @Test
    void echoesOrigin() throws Exception {
        stubGet("/providers/httpbin", fixture("httpbin.json"));

        mockMvc.perform(get("/api/v1/echo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origin").value("203.0.113.10"));
        server.verify();
    }

    @Test
    void looksUpPostalCode() throws Exception {
        stubGet("/providers/zippopotam/us/90210", fixture("zippopotam-90210.json"));

        mockMvc.perform(get("/api/v1/postal/us/90210"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places[0].placeName").value("Beverly Hills"));
        server.verify();
    }

    @Test
    void reportsUnknownPostalCode() throws Exception {
        server.expect(requestTo(containsString("/providers/zippopotam/us/00000")))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"status":404,"error":"Not Found","message":"Postal code was not found.","provider":"zippopotam"}
                                """));

        mockMvc.perform(get("/api/v1/postal/us/00000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(RequestRules.NOT_FOUND_POSTAL))
                .andExpect(jsonPath("$.provider").value("zippopotam"));
        server.verify();
    }

    @Test
    void rejectsInvalidPostalPath() throws Exception {
        mockMvc.perform(get("/api/v1/postal/usa/90210"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(RequestRules.POSTAL_INVALID))
                .andExpect(jsonPath("$.provider").value("zippopotam"));
        server.verify();
    }

    @Test
    void requiresCoinIds() throws Exception {
        mockMvc.perform(get("/api/v1/crypto/price"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(RequestRules.IDS_INVALID))
                .andExpect(jsonPath("$.provider").value("coinmarketcap"));
        server.verify();
    }

    @Test
    void rejectsTickerSymbol() throws Exception {
        mockMvc.perform(get("/api/v1/crypto/price").queryParam("ids", "BTC"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(RequestRules.IDS_INVALID));
        server.verify();
    }

    @Test
    void returnsCryptoPrice() throws Exception {
        stubGet("/providers/coinmarketcap/price", fixture("cmc-price.json"));

        mockMvc.perform(get("/api/v1/crypto/price").queryParam("ids", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.prices[0].id").value(1));
        server.verify();
    }

    @Test
    void passesThroughUpstreamFailure() throws Exception {
        server.expect(requestTo(containsString("/providers/coinmarketcap/price")))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"status":502,"error":"Bad Gateway","message":"Upstream response was not usable.","provider":"coinmarketcap"}
                                """));

        mockMvc.perform(get("/api/v1/crypto/price").queryParam("ids", "1"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value(RequestRules.UNUSABLE));
        server.verify();
    }

    @Test
    void reportsIntegrationLayerDown() throws Exception {
        server.expect(requestTo(containsString("/providers/ipify")))
                .andRespond(request -> {
                    throw new ResourceAccessException("connect failed", new ConnectException("Connection refused"));
                });

        mockMvc.perform(get("/api/v1/ip"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(RequestRules.UNAVAILABLE))
                .andExpect(jsonPath("$.provider").value("ipify"));
        server.verify();
    }

    @Test
    void integratesThroughTheGeneralRoute() throws Exception {
        String envelope = """
                {"provider":"agify","operation":"predict-age","data":%s}
                """.formatted(fixture("agify.json"));
        server.expect(requestTo(containsString("/integrate")))
                .andRespond(withSuccess(envelope, MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/api/v1/integrate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"agify","operation":"predict-age","params":{"name":"michael"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("predict-age"))
                .andExpect(jsonPath("$.data.name").value("michael"));
        server.verify();
    }

    @Test
    void rejectsUnknownProvider() throws Exception {
        mockMvc.perform(post("/api/v1/integrate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"nope\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(RequestRules.INTEGRATION_INVALID));
        server.verify();
    }

    @Test
    void rejectsZipWithoutPostalCode() throws Exception {
        mockMvc.perform(post("/api/v1/integrate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"zippopotam\",\"params\":{\"country\":\"us\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(RequestRules.INTEGRATION_INVALID));
        server.verify();
    }

    @Test
    void rejectsPlainTextIntegrate() throws Exception {
        mockMvc.perform(post("/api/v1/integrate").contentType(MediaType.TEXT_PLAIN).content("hi"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value(RequestRules.CONTENT_TYPE));
        server.verify();
    }

    @Test
    void rejectsEmptyIntegrateBody() throws Exception {
        mockMvc.perform(post("/api/v1/integrate").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(RequestRules.INTEGRATION_INVALID));
        server.verify();
    }

    private void stubGet(String path, String body) throws IOException {
        server.expect(requestTo(containsString(path)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }
}
