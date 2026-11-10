package com.yitinsun.muleintegration.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "CryptoPrice", description = "One CoinMarketCap id. Optional fields are omitted when not requested.")
public record CryptoPrice(
        long id,
        BigDecimal price,
        BigDecimal marketCap,
        BigDecimal volume24h,
        BigDecimal percentChange24h,
        String lastUpdated) {
}
