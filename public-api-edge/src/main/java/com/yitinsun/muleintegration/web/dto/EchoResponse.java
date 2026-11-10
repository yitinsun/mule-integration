package com.yitinsun.muleintegration.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(name = "EchoResponse", description = "HTTPBin GET /get echo")
public record EchoResponse(
        String url,
        String origin,
        Map<String, String> headers,
        Map<String, String> args) {
}
