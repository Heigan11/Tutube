package com.tutube.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Email verification request")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationRequest {
    @Schema(description = "Email address to verify", example = "user@example.com", required = true)
    private String email;
    @Schema(description = "Verification code received via email", example = "123456", required = true)
    private String code;
}