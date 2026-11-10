package com.yitinsun.muleintegration.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private final int serverPort;

    public OpenApiConfig(@Value("${server.port:8080}") int serverPort) {
        this.serverPort = serverPort;
    }

    @Bean
    public OpenAPI publicApiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Public API edge")
                        .description(
                                "Specific tasks and POST /api/v1/integrate. "
                                        + "The edge calls local Mule only. "
                                        + "Mule calls IPify, Agify, HTTPBin, Zippopotam, and CoinMarketCap.")
                        .version("0.1.0"))
                .addServersItem(new Server()
                        .url("http://localhost:" + serverPort)
                        .description("Local edge"));
    }
}
