package com.tutube.core.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutube.core.dto.ApiResponseTutube;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static com.tutube.core.utils.ErrorTypes.*;

@Component
public class SecurityErrorHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Mono<Void> handleAuthenticationError(ServerWebExchange exchange, Exception ex) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiResponseTutube<?> apiResponseTutube = ApiResponseTutube.error(
                "Authentication failed: Invalid or missing token",
                INVALID_TOKEN
        );

        return writeResponse(exchange, apiResponseTutube);
    }

    public Mono<Void> handleAccessDeniedError(ServerWebExchange exchange, Exception ex) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiResponseTutube<?> apiResponseTutube = ApiResponseTutube.error(
                "Access denied",
                ACCESS_DENIED
        );

        return writeResponse(exchange, apiResponseTutube);
    }

    private Mono<Void> writeResponse(ServerWebExchange exchange, ApiResponseTutube<?> apiResponseTutube) {
        try {
            String responseBody = objectMapper.writeValueAsString(apiResponseTutube);
            DataBuffer buffer = exchange.getResponse()
                    .bufferFactory()
                    .wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            // Fallback
            String fallbackResponse = "{\"message\":" + apiResponseTutube.getMessage() + ",\"success\""+ apiResponseTutube.isSuccess() + ",\"errorType\":" + apiResponseTutube.getErrorType() + "}";
            DataBuffer buffer = exchange.getResponse()
                    .bufferFactory()
                    .wrap(fallbackResponse.getBytes());
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }
    }
}
