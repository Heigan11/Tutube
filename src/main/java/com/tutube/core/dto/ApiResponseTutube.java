package com.tutube.core.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Standard API response")
public class ApiResponseTutube<T> {

    @Schema(description = "Response message")
    private String message;

    @Schema(description = "Success flag")
    private boolean success;

    @Schema(description = "Response data")
    private T data;

    @Schema(description = "Error type if any")
    private String errorType;

    @Schema(description = "JWT token for authentication")
    private String token;

    // Статические factory методы для успешных ответов
    public static <T> ApiResponseTutube<T> success(String message, T data) {
        return ApiResponseTutube.<T>builder()
                .message(message)
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ApiResponseTutube<T> success(String message, T data, String token) {
        return ApiResponseTutube.<T>builder()
                .message(message)
                .success(true)
                .data(data)
                .token(token)
                .build();
    }

    public static ApiResponseTutube<Void> success(String message) {
        return ApiResponseTutube.<Void>builder()
                .message(message)
                .success(true)
                .build();
    }

    // Статические factory методы для ошибок
    public static <T> ApiResponseTutube<T> error(String message, String errorType) {
        return ApiResponseTutube.<T>builder()
                .message(message)
                .success(false)
                .errorType(errorType)
                .build();
    }

    public static <T> ApiResponseTutube<T> error(String message, String errorType, T data) {
        return ApiResponseTutube.<T>builder()
                .message(message)
                .success(false)
                .data(data)
                .errorType(errorType)
                .build();
    }
}
