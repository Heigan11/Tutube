package com.tutube.core.controllersTest;

import com.tutube.core.conrollers.UserController;
import com.tutube.core.dto.ApiResponseTutube;
import com.tutube.core.dto.UserDto;
import com.tutube.core.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(UserControllerIntegrationTest.class);

    @Autowired
    private UserController userController;

    @Autowired
    private UserRepository userRepository;

    @Test
    void getUserByUserName_success() {
        log.info("=== Интеграционный тест: успешный запрос своих данных ===");

        // given
        String userName = "testUser";
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                userName, "password", Collections.emptyList()
        );

        log.info("1. Проверяем наличие пользователя {} в БД", userName);
        StepVerifier.create(userRepository.findByUserName(userName))
                .expectNextMatches(user -> {
                    assertThat(user.getUsername()).isEqualTo(userName);
                    return true;
                })
                .verifyComplete();

        // when
        log.info("2. Вызываем контроллер с реальными данными из БД");
        Mono<ResponseEntity<ApiResponseTutube<UserDto>>> result = userController
                .getUserByUserName(userName, userDetails);

        // then
        log.info("3. Проверяем результат запроса");
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    log.info("4. Статус ответа: {}", response.getStatusCode());
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody().isSuccess()).isTrue();
                    assertThat(response.getBody().getData().getUserName()).isEqualTo(userName);
                    log.info("5. Данные пользователя получены успешно");
                    return true;
                })
                .verifyComplete();

        log.info("=== Интеграционный тест завершен ===");
    }

    @Test
    void getUserByUserName_accessDenied() {
        log.info("=== Интеграционный тест: доступ запрещен ===");

        // given
        String existingUserName = "testUser"; // существует в БД
        String otherUserName = "admin";       // другой пользователь в БД
        String currentUserName = "hacker";    // текущий пользователь (не совпадает)

        UserDetails currentUserDetails = new org.springframework.security.core.userdetails.User(
                currentUserName, "password", Collections.emptyList()
        );

        log.info("1. Проверяем наличие пользователя {} в БД", existingUserName);
        StepVerifier.create(userRepository.findByUserName(existingUserName))
                .expectNextCount(1)
                .verifyComplete();

        log.info("2. Проверяем наличие пользователя {} в БД", otherUserName);
        StepVerifier.create(userRepository.findByUserName(otherUserName))
                .expectNextCount(1)
                .verifyComplete();

        // when
        log.info("3. Вызываем контроллер - пользователь {} пытается получить данные {}",
                currentUserName, existingUserName);
        Mono<ResponseEntity<ApiResponseTutube<UserDto>>> result = userController
                .getUserByUserName(existingUserName, currentUserDetails);

        // then
        log.info("4. Проверяем, что доступ запрещен (403 Forbidden)");
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    log.info("5. Статус ответа: {}", response.getStatusCode());
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(response.getBody().isSuccess()).isFalse();
                    assertThat(response.getBody().getErrorType()).isEqualTo("ACCESS_DENIED");
                    assertThat(response.getBody().getMessage()).isEqualTo("Access denied");
                    log.info("6. Доступ запрещен - интеграционный тест пройден");
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getUserByUserName_userNotFound() {
        log.info("=== Интеграционный тест: пользователь не найден ===");

        // given
        String nonExistentUserName = "nonExistentUser";
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                nonExistentUserName, "password", Collections.emptyList()
        );

        log.info("1. Проверяем, что пользователя {} нет в БД", nonExistentUserName);
        StepVerifier.create(userRepository.findByUserName(nonExistentUserName))
                .expectNextCount(0)
                .verifyComplete();

        // when
        log.info("2. Вызываем контроллер для несуществующего пользователя: {}", nonExistentUserName);
        Mono<ResponseEntity<ApiResponseTutube<UserDto>>> result = userController
                .getUserByUserName(nonExistentUserName, userDetails);

        // then
        log.info("3. Проверяем, что пользователь не найден (403 Forbidden)");
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    log.info("4. Статус ответа: {}", response.getStatusCode());
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(response.getBody().isSuccess()).isFalse();
                    assertThat(response.getBody().getErrorType()).isEqualTo("ACCESS_DENIED");
                    log.info("5. Пользователь не найден - интеграционный тест пройден");
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getUserByUserName_unauthorized() {
        log.info("=== Тест: неавторизованный доступ ===");

        // given
        String userName = "testUser";

        log.info("1. Вызываем контроллер с null userDetails");
        Mono<ResponseEntity<ApiResponseTutube<UserDto>>> result = userController
                .getUserByUserName(userName, null);

        // when & then
        log.info("2. Проверяем, что получаем ошибку (NPE в фильтре)");
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> {
                    log.info("3. Ожидаемая ошибка: {}", throwable.getClass().getSimpleName());
                    assertThat(throwable).isInstanceOf(NullPointerException.class);
                    log.info("4. Неавторизованный доступ - тест пройден");
                    return true;
                })
                .verify();
    }
}