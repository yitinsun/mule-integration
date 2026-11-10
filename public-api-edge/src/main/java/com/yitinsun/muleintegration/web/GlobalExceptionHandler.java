package com.yitinsun.muleintegration.web;

import com.yitinsun.muleintegration.domain.RequestRules;
import com.yitinsun.muleintegration.integration.UpstreamException;
import com.yitinsun.muleintegration.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UpstreamException.class)
    ResponseEntity<ErrorResponse> upstream(UpstreamException ex) {
        return respond(ex.status(), ex.getMessage(), ex.provider());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), provider(request));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException ex) {
        return respond(HttpStatus.BAD_REQUEST.value(), RequestRules.INTEGRATION_INVALID, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponse> mediaType(HttpMediaTypeNotSupportedException ex) {
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), RequestRules.CONTENT_TYPE, null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> fallback(Exception ex, HttpServletRequest request) {
        String provider = provider(request);
        log.error("status=502 provider={}", provider, ex);
        return respond(HttpStatus.BAD_GATEWAY.value(), RequestRules.UNUSABLE, provider);
    }

    private ResponseEntity<ErrorResponse> respond(int status, String message, String provider) {
        log.info("status={} provider={}", status, provider);
        ErrorResponse body = new ErrorResponse(status, RequestRules.phrase(status), message, provider);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    private static String provider(HttpServletRequest request) {
        Object attribute = request.getAttribute(RequestRules.PROVIDER_ATTR);
        if (attribute instanceof String value && !value.isBlank()) {
            return value;
        }
        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/ip")) {
            return "ipify";
        }
        if (path.startsWith("/api/v1/age")) {
            return "agify";
        }
        if (path.startsWith("/api/v1/echo")) {
            return "httpbin";
        }
        if (path.startsWith("/api/v1/postal")) {
            return "zippopotam";
        }
        if (path.startsWith("/api/v1/crypto")) {
            return "coinmarketcap";
        }
        return null;
    }
}
