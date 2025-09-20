package com.tutube.core.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutube.core.dto.ApiResponse;
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

        ApiResponse<?> apiResponse = ApiResponse.error(
                "Authentication failed: Invalid or missing token",
                INVALID_TOKEN
        );

        return writeResponse(exchange, apiResponse);
    }

    public Mono<Void> handleAccessDeniedError(ServerWebExchange exchange, Exception ex) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiResponse<?> apiResponse = ApiResponse.error(
                "Access denied",
                ACCESS_DENIED
        );

        return writeResponse(exchange, apiResponse);
    }

    private Mono<Void> writeResponse(ServerWebExchange exchange, ApiResponse<?> apiResponse) {
        try {
            String responseBody = objectMapper.writeValueAsString(apiResponse);
            DataBuffer buffer = exchange.getResponse()
                    .bufferFactory()
                    .wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            // Fallback
            String fallbackResponse = "{\"message\":" + apiResponse.getMessage() + ",\"success\""+ apiResponse.isSuccess() + ",\"errorType\":" + apiResponse.getErrorType() + "}";
            DataBuffer buffer = exchange.getResponse()
                    .bufferFactory()
                    .wrap(fallbackResponse.getBytes());
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }
    }
}
