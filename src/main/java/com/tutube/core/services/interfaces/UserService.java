package com.tutube.core.services.interfaces;

import com.tutube.core.dto.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserService {

    Mono<User> getUserByUserName(String userName);
    Mono<User> updateUser(User user);
}