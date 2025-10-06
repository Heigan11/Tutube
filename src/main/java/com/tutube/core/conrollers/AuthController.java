package com.tutube.core.conrollers;

import com.tutube.core.configuration.JwtUtil;
import com.tutube.core.dto.*;
import com.tutube.core.repositories.UserRepository;
import com.tutube.core.services.interfaces.EmailVerificationService;
import com.tutube.core.services.interfaces.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;

import static com.tutube.core.utils.ErrorTypes.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register")
    public Mono<ResponseEntity<ApiResponseTutube<Void>>> register(@RequestBody RegistrationRequest request) {
        // 1. Проверяем валидность email
        if (!isValidEmail(request.getUserName())) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(ApiResponseTutube.error("Invalid email format", VALIDATION_ERROR)));
        }

        // 2. Проверяем существование пользователя
        return userRepository.findByUserName(request.getUserName())
                .flatMap(existingUser -> Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponseTutube.<Void>error("User already exists", USER_ALREADY_EXISTS))))
                .switchIfEmpty(Mono.<ResponseEntity<ApiResponseTutube<Void>>>defer(() ->
                        emailVerificationService.generateAndSendVerificationCode(request.getUserName())
                                .map(code -> ResponseEntity.ok()
                                        .body(ApiResponseTutube.<Void>success("Verification code sent to email", null)))
                                .onErrorResume(error -> Mono.just(ResponseEntity.badRequest()
                                        .body(ApiResponseTutube.<Void>error(error.getMessage(), VALIDATION_ERROR))))
                ));
    }

    @PostMapping("/verify")
    public Mono<ResponseEntity<ApiResponseTutube<UserDto>>> verifyEmail(@RequestBody EmailVerificationRequest request) {
        return emailVerificationService.verifyCode(request.getEmail(), request.getCode())
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Mono.just(ResponseEntity.badRequest()
                                .body(ApiResponseTutube.error("Invalid or expired code", VALIDATION_ERROR)));
                    }

                    // Создаем пользователя после успешной верификации
                    User user = new User(request.getEmail(), passwordEncoder.encode(request.getCode()));
                    return userRepository.save(user)
                            .map(savedUser -> {
                                String token = jwtUtil.generateToken(savedUser.getUsername());
                                UserDto userDto = UserDto.convertToUserDto(savedUser);
                                return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(ApiResponseTutube.success("User registered successfully", userDto, token));
                            });
                });
    }

    @Operation(
            summary = "User login",
            description = "Authenticate user and return JWT token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public Mono<ResponseEntity<ApiResponseTutube<UserDto>>> login(
            @Parameter(description = "Login credentials", required = true)
            @RequestBody LoginRequest request) {
        return userRepository.findByUserName(request.getUserName())
                .flatMap(user -> {
                    if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        String token = jwtUtil.generateToken(user.getUsername());
                        UserDto userDto = UserDto.convertToUserDto(user);
                        return Mono.just(ResponseEntity.ok(
                                ApiResponseTutube.success("Login successful", userDto, token)));
                    } else {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponseTutube.<UserDto>error("Invalid credentials", INVALID_CREDENTIALS)));
                    }
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponseTutube.error("Invalid credentials", INVALID_CREDENTIALS))));
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email != null && email.matches(emailRegex);
    }

    @Schema(description = "Login request")
    @Data
    public static class LoginRequest {
        @Schema(description = "Username (email)", example = "testUser@test.com", required = true)
        private String userName;

        @Schema(description = "Password", example = "123456", required = true)
        private String password;
    }
}
