package com.yitinsun.muleintegration.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AgeResponse", description = "Agify age prediction")
public record AgeResponse(
        @Schema(example = "michael") String name,
        @Schema(description = "Predicted age. Null when Agify has no guess.", nullable = true) Integer age,
        @Schema(description = "Number of samples Agify used") long count) {
}
