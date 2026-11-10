package com.yitinsun.muleintegration.web;

import com.yitinsun.muleintegration.domain.Provider;
import com.yitinsun.muleintegration.domain.RequestRules;
import com.yitinsun.muleintegration.integration.MuleClient;
import com.yitinsun.muleintegration.web.dto.ErrorResponse;
import com.yitinsun.muleintegration.web.dto.IpResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "IP", description = "Public IP from IPify")
public class IpController {

    private final MuleClient muleClient;

    public IpController(MuleClient muleClient) {
        this.muleClient = muleClient;
    }

    @Operation(
            summary = "Read the public IP",
            description = "JSON by default. The same GET returns text/plain when format=text.")
    @ApiResponse(responseCode = "200",
            content = @Content(schema = @Schema(implementation = IpResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid format or unknown query",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "Mule is down",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/api/v1/ip")
    public ResponseEntity<?> ip(HttpServletRequest request) {
        RequestRules.rejectUnknownQuery(request.getParameterMap(), Provider.IPIFY.queryKeys());
        String format = RequestRules.format(request.getParameter("format"));
        IpResponse body = muleClient.publicIp(format);
        if ("text".equals(format)) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(body.ip());
        }
        return ResponseEntity.ok(body);
    }
}
