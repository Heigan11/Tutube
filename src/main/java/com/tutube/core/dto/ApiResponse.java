package com.tutube.core.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    private String message;
    private boolean success;
    private T data;
    private String errorType;
    private String token;

    // Статические factory методы для успешных ответов
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .message(message)
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data, String token) {
        return ApiResponse.<T>builder()
                .message(message)
                .success(true)
                .data(data)
                .token(token)
                .build();
    }

    public static ApiResponse<Void> success(String message) {
        return ApiResponse.<Void>builder()
                .message(message)
                .success(true)
                .build();
    }

    // Статические factory методы для ошибок
    public static <T> ApiResponse<T> error(String message, String errorType) {
        return ApiResponse.<T>builder()
                .message(message)
                .success(false)
                .errorType(errorType)
                .build();
    }

    public static <T> ApiResponse<T> error(String message, String errorType, T data) {
        return ApiResponse.<T>builder()
                .message(message)
                .success(false)
                .data(data)
                .errorType(errorType)
                .build();
    }
}
