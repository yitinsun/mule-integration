package com.yitinsun.muleintegration.web;

import com.yitinsun.muleintegration.domain.Provider;
import com.yitinsun.muleintegration.domain.RequestRules;
import com.yitinsun.muleintegration.integration.MuleClient;
import com.yitinsun.muleintegration.web.dto.AgeResponse;
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
@Tag(name = "Age", description = "Age prediction from Agify")
public class AgeController {

    private final MuleClient muleClient;

    public AgeController(MuleClient muleClient) {
        this.muleClient = muleClient;
    }

    @Operation(summary = "Predict an age from a name")
    @ApiResponse(responseCode = "200",
            content = @Content(schema = @Schema(implementation = AgeResponse.class)))
    @ApiResponse(responseCode = "400", description = "Missing or invalid name",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "Mule is down",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/api/v1/age")
    public AgeResponse age(HttpServletRequest request) {
        RequestRules.rejectUnknownQuery(request.getParameterMap(), Provider.AGIFY.queryKeys());
        String name = RequestRules.name(request.getParameter("name"));
        return muleClient.age(name);
    }
}
