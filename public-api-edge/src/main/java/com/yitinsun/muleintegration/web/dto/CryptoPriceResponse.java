package com.yitinsun.muleintegration.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "CryptoPriceResponse", description = "Simple prices in one currency")
public record CryptoPriceResponse(
        @Schema(example = "USD") String currency,
        List<CryptoPrice> prices) {
}
