package com.tutube.core.conrollers;


import com.tutube.core.dto.User;
import com.tutube.core.dto.UserDto;
import com.tutube.core.repositories.UserRepository;
import com.tutube.core.services.interfaces.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/{userName}")
    public Mono<ResponseEntity<UserDto>> getUserByUserName(
            @PathVariable String userName,
            @AuthenticationPrincipal UserDetails userDetails) {

        return userService.getUserByUserName(userName)
                .filter(user -> user.getUsername().equals(userDetails.getUsername()))
                .map(user -> ResponseEntity.ok(UserDto.convertToUserDto(user)))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.FORBIDDEN).build())
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }


    // Обновить пользователя (ID берется из тела запроса)
//    @PutMapping
////    @PreAuthorize("hasRole('USER') and #user.email == authentication.name")
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
    public Mono<ResponseEntity<?>> updateUser(
            @RequestBody User user,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (user.getUsername() == null) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        // Проверяем, что пользователь обновляет свои данные
        return userRepository.findByUserName(user.getUsername())
                .flatMap(existingUser -> {
                    if (!existingUser.getUsername().equals(userDetails.getUsername())) {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
                    }
                    return userService.updateUser(user)
                            .map(updatedUser -> ResponseEntity.ok(UserDto.convertToUserDto(updatedUser)));
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
