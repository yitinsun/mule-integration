package com.yitinsun.muleintegration.web;

import com.yitinsun.muleintegration.domain.Provider;
import com.yitinsun.muleintegration.domain.RequestRules;
import com.yitinsun.muleintegration.integration.MuleClient;
import com.yitinsun.muleintegration.web.dto.ErrorResponse;
import com.yitinsun.muleintegration.web.dto.IntegrateRequest;
import com.yitinsun.muleintegration.web.dto.IntegrateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Integrate", description = "One operation for all five providers")
public class IntegrateController {

    private final MuleClient muleClient;

    public IntegrateController(MuleClient muleClient) {
        this.muleClient = muleClient;
    }

    @Operation(summary = "Call a provider through Mule")
    @ApiResponse(responseCode = "200",
            content = @Content(schema = @Schema(implementation = IntegrateResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid provider, operation, or params",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "415", description = "Content-Type must be application/json",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "Mule is down",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(path = "/api/v1/integrate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public IntegrateResponse integrate(@RequestBody IntegrateRequest body, HttpServletRequest request) {
        Provider provider = RequestRules.provider(body.provider());
        request.setAttribute(RequestRules.PROVIDER_ATTR, provider.id());
        String operation = RequestRules.operation(provider, body.operation());
        Map<String, Object> params = RequestRules.params(provider, body.params());
        return muleClient.integrate(provider.id(), operation, params);
    }
}
