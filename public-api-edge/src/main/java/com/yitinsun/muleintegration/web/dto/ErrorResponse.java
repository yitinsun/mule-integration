package com.yitinsun.muleintegration.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ErrorResponse", description = "JSON error body")
public record ErrorResponse(
        int status,
        String error,
        String message,
        @Schema(description = "Provider id when the failure is tied to one", nullable = true) String provider) {
}
