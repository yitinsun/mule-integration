package com.yitinsun.muleintegration.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "IpResponse", description = "Public IP as seen by Mule")
public record IpResponse(
        @Schema(description = "Address reported by IPify", example = "203.0.113.10") String ip) {
}
