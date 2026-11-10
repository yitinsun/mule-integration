package com.yitinsun.muleintegration.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "IntegrateResponse", description = "Normalized result of the general operation")
public record IntegrateResponse(
        String provider,
        String operation,
        @Schema(description = "Provider payload after Mule normalization") JsonNode data) {
}
