package com.yitinsun.muleintegration.web;

import com.yitinsun.muleintegration.domain.Provider;
import com.yitinsun.muleintegration.domain.RequestRules;
import com.yitinsun.muleintegration.integration.MuleClient;
import com.yitinsun.muleintegration.web.dto.CryptoPriceResponse;
import com.yitinsun.muleintegration.web.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Crypto", description = "Keyless CoinMarketCap simple price")
public class CryptoPriceController {

    private final MuleClient muleClient;

    public CryptoPriceController(MuleClient muleClient) {
        this.muleClient = muleClient;
    }

    @Operation(summary = "Read simple prices by CoinMarketCap id")
    @ApiResponse(responseCode = "200",
            content = @Content(schema = @Schema(implementation = CryptoPriceResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid ids or flag",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "429", description = "Upstream rate limit",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "502", description = "Upstream response was not usable",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "Mule is down",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/api/v1/crypto/price")
    public CryptoPriceResponse price(HttpServletRequest request) {
        RequestRules.rejectUnknownQuery(request.getParameterMap(), Provider.COINMARKETCAP.queryKeys());
        String ids = request.getParameter("ids");
        RequestRules.coinIds(ids);
        return muleClient.price(
                ids,
                RequestRules.flag(request.getParameter("includeMarketCap")),
                RequestRules.flag(request.getParameter("includeVolume24h")),
                RequestRules.flag(request.getParameter("includePercentChange24h")),
                RequestRules.flag(request.getParameter("includeLastUpdated")));
    }
}
