package com.tutube.core.conrollers;

import com.tutube.core.dto.ApiResponseTutube;
import com.tutube.core.dto.User;
import com.tutube.core.dto.UserDto;
import com.tutube.core.repositories.UserRepository;
import com.tutube.core.services.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.tutube.core.utils.ErrorTypes.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "APIs for user management operations")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @Operation(
            summary = "Get user by username",
            description = "Returns user details by username. Requires authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - access denied")
    })
    @GetMapping("/{userName}")
    public Mono<ResponseEntity<ApiResponseTutube<UserDto>>> getUserByUserName(
            @Parameter(description = "Username of the user", required = true, example = "testUser@test.com")
            @PathVariable String userName,
            @AuthenticationPrincipal UserDetails userDetails) {

        return userService.getUserByUserName(userName)
                .filter(user -> user.getUsername().equals(userDetails.getUsername()))
                .map(UserDto::convertToUserDto)
                .map(userDto -> ResponseEntity.ok(ApiResponseTutube.success("User found", userDto)))
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponseTutube.error("Access denied", ACCESS_DENIED))));
    }


    // Обновить пользователя (ID берется из тела запроса)
//    @PutMapping

    /// /    @PreAuthorize("hasRole('USER') and #user.email == authentication.name")
//    @PreAuthorize("#user.email == authentication.name")
//    public Mono<ResponseEntity<User>> updateUser(@RequestBody User user) {
//        if (user.getId() == null) {
//            return Mono.just(ResponseEntity.badRequest().build());
//        }
//        return userService.updateUser(user)
//                .map(ResponseEntity::ok)
//                .defaultIfEmpty(ResponseEntity.notFound().build());
//    }

    @PutMapping
    public Mono<ResponseEntity<ApiResponseTutube<UserDto>>> updateUser(
            @RequestBody User user,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (user.getUsername() == null) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(ApiResponseTutube.error("Username is required", VALIDATION_ERROR)));
        }

        return userRepository.findByUserName(user.getUsername())
                .switchIfEmpty(Mono.error(new RuntimeException("Access denied")))
                .flatMap(existingUser -> {
                    if (!existingUser.getUsername().equals(userDetails.getUsername())) {
                        return Mono.error(new RuntimeException("Access denied"));
                    }
                    return userService.updateUser(user);
                })
                .map(UserDto::convertToUserDto)
                .map(updatedUser -> ResponseEntity.ok(
                        ApiResponseTutube.success("User updated successfully", updatedUser)))
                .onErrorResume(error -> {
                    if (error.getMessage().equals("Access denied")) {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(ApiResponseTutube.error("Access denied", ACCESS_DENIED)));
                    } else {
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponseTutube.error("Update failed", INTERNAL_ERROR)));
                    }
                });
    }
}
