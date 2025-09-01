package com.tutube.core.conrollers;

import com.tutube.core.configuration.JwtUtil;
import com.tutube.core.dto.User;
import com.tutube.core.repositories.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public Mono<ResponseEntity<AuthResponse>> register(@RequestBody RegistrationRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .flatMap(existingUser -> Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new AuthResponse("User already exists", false, null))))
                .switchIfEmpty(Mono.defer(() -> {
                    User user = new User();
                    user.setFirstName(request.getFirstName());
                    user.setLastName(request.getLastName());
                    user.setEmail(request.getEmail());
                    user.setPassword(passwordEncoder.encode(request.getPassword()));
                    user.setAge(request.getAge());

                    return userRepository.save(user)
                            .map(savedUser -> {
                                String token = jwtUtil.generateToken(savedUser.getEmail());
                                return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(new AuthResponse("User registered successfully", true, token));
                            });
                }));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody LoginRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .flatMap(user -> {
                    if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        String token = jwtUtil.generateToken(user.getEmail());
                        return Mono.just(ResponseEntity.ok(
                                new AuthResponse("Login successful", true, token)));
                    } else {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(new AuthResponse("Invalid password", false, null)));
                    }
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new AuthResponse("User not found", false, null))));
    }

    @Data
    public static class RegistrationRequest {
        private String firstName;
        private String lastName;
        private String email;
        private String password;
        private double age;
    }

    @Data
    public static class LoginRequest {
        private String email;
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
