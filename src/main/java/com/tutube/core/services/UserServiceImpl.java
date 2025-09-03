package com.tutube.core.services;

import com.tutube.core.dto.User;
import com.tutube.core.repositories.UserRepository;
import com.tutube.core.services.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public Flux<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Mono<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Mono<User> findByUserName(String userName) { // Новый метод
        return userRepository.findByUserName(userName);
    }

    @Override
    public Mono<User> createUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public Mono<User> updateUser(User user) {
        return userRepository.findByUserName(user.getUsername())
                .flatMap(existingUser -> {
                    // Обновляем только разрешенные поля (исключаем email и пароль)
                    if (user.getFirstName() != null) {
                        existingUser.setFirstName(user.getFirstName());
                    }
                    if (user.getLastName() != null) {
                        existingUser.setLastName(user.getLastName());
                    }
                    if (user.getAge() != null) {
                        existingUser.setAge(user.getAge());
                    }
                    if (user.getFactAge() != null) {
                        existingUser.setFactAge(user.getFactAge());
                    }
                    if (user.getLevel() != 0) {
                        existingUser.setLevel(user.getLevel());
                    }
                    if (user.getSuccessRate() != null) {
                        existingUser.setSuccessRate(user.getSuccessRate());
                    }
                    if (user.getAttemptsCount() != null) {
                        existingUser.setAttemptsCount(user.getAttemptsCount());
                    }
                    return userRepository.save(existingUser);
                }).switchIfEmpty(Mono.empty());
    }
}
