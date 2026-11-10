package com.yitinsun.muleintegration.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(name = "IntegrateRequest", description = "General provider call")
public record IntegrateRequest(
        @Schema(description = "ipify, agify, httpbin, zippopotam, or coinmarketcap",
                example = "agify", requiredMode = Schema.RequiredMode.REQUIRED)
        String provider,
        @Schema(description = "Omit to use the provider default operation", example = "predict-age")
        String operation,
        @Schema(description = "Flat string or scalar params for that provider")
        Map<String, Object> params) {
}
