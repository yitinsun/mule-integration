package com.yitinsun.muleintegration.web;

import com.yitinsun.muleintegration.domain.Provider;
import com.yitinsun.muleintegration.domain.RequestRules;
import com.yitinsun.muleintegration.integration.MuleClient;
import com.yitinsun.muleintegration.web.dto.ErrorResponse;
import com.yitinsun.muleintegration.web.dto.PostalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Postal", description = "Zippopotam postal lookup")
public class PostalController {

    private final MuleClient muleClient;

    public PostalController(MuleClient muleClient) {
        this.muleClient = muleClient;
    }

    @Operation(summary = "Look up a postal code")
    @ApiResponse(responseCode = "200",
            content = @Content(schema = @Schema(implementation = PostalResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid country or postal code",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Postal code was not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "Mule is down",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/api/v1/postal/{country}/{postalCode}")
    public PostalResponse postal(
            @PathVariable String country,
            @PathVariable String postalCode,
            HttpServletRequest request) {
        RequestRules.rejectUnknownQuery(request.getParameterMap(), Provider.ZIPPOPOTAM.queryKeys());
        return muleClient.postal(RequestRules.country(country), RequestRules.postalCode(postalCode));
    }
}
