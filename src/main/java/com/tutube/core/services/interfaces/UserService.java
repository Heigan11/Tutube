package com.tutube.core.services.interfaces;

import com.tutube.core.dto.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserService {
    Flux<User> getAllUsers();
    Mono<User> getUserById(Long id);
    Mono<User> createUser(User user);
    Mono<User> updateUser(User user);
}