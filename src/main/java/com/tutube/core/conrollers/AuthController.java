package com.tutube.core.conrollers;

import com.tutube.core.configuration.JwtUtil;
import com.tutube.core.dto.ApiResponse;
import com.tutube.core.dto.User;
import com.tutube.core.dto.UserDto;
import com.tutube.core.repositories.UserRepository;
import com.tutube.core.services.interfaces.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static com.tutube.core.utils.ErrorTypes.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @PostMapping("/register")
    public Mono<ResponseEntity<ApiResponse<UserDto>>> register(@RequestBody RegistrationRequest request) {
        return userRepository.findByUserName(request.getUserName())
                .flatMap(existingUser -> Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.<UserDto>error("User already exists", USER_ALREADY_EXISTS))))
                .switchIfEmpty(Mono.defer(() -> {
                    User user = new User(request.getUserName(), passwordEncoder.encode(request.getPassword()));

                    return userRepository.save(user)
                            .map(savedUser -> {
                                String token = jwtUtil.generateToken(savedUser.getUsername());
                                UserDto userDto = UserDto.convertToUserDto(savedUser);
                                return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(ApiResponse.success("User registered successfully", userDto, token));
                            });
                }));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<ApiResponse<UserDto>>> login(@RequestBody LoginRequest request) {
        return userRepository.findByUserName(request.getUserName())
                .flatMap(user -> {
                    if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        String token = jwtUtil.generateToken(user.getUsername());
                        UserDto userDto = UserDto.convertToUserDto(user);
                        return Mono.just(ResponseEntity.ok(
                                ApiResponse.success("Login successful", userDto, token)));
                    } else {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponse.<UserDto>error("Invalid credentials", INVALID_CREDENTIALS)));
                    }
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid credentials", INVALID_CREDENTIALS))));
    }

    @Data
    public static class RegistrationRequest {
        private String userName;
        private String password;
    }

    @Data
    public static class LoginRequest {
        private String userName;
        private String password;
    }

    @Data
    public static class AuthResponse {
        private String message;
        private boolean success;
        private String token;

        public AuthResponse(String message, boolean success, String token) {
            this.message = message;
            this.success = success;
            this.token = token;
        }
    }
}
