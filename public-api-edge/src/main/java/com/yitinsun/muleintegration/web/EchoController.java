package com.yitinsun.muleintegration.web;

import com.yitinsun.muleintegration.domain.Provider;
import com.yitinsun.muleintegration.domain.RequestRules;
import com.yitinsun.muleintegration.integration.MuleClient;
import com.yitinsun.muleintegration.web.dto.EchoResponse;
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
@Tag(name = "Echo", description = "HTTPBin GET echo")
public class EchoController {

    private final MuleClient muleClient;

    public EchoController(MuleClient muleClient) {
        this.muleClient = muleClient;
    }

    @Operation(summary = "Echo the call through HTTPBin")
    @ApiResponse(responseCode = "200",
            content = @Content(schema = @Schema(implementation = EchoResponse.class)))
    @ApiResponse(responseCode = "400", description = "Unknown query",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "Mule is down",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/api/v1/echo")
    public EchoResponse echo(HttpServletRequest request) {
        RequestRules.rejectUnknownQuery(request.getParameterMap(), Provider.HTTPBIN.queryKeys());
        return muleClient.echo();
    }
}
