package com.tutube.core.conrollers;


import com.tutube.core.dto.User;
import com.tutube.core.repositories.UserRepository;
import com.tutube.core.services.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    // Получить всех пользователей
    @GetMapping
    public Flux<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // Получить пользователя по ID
    @GetMapping("/{id}")
    public Mono<ResponseEntity<User>> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // Создать нового пользователя
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<User> createUser(@RequestBody User user) {
        return userService.createUser(user);
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
                            .map(ResponseEntity::ok);
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

}
