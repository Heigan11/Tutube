package com.tutube.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "User registration request")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequest {
    @Schema(description = "User email address", example = "user@example.com", required = true)
    private String userName; // email
}
