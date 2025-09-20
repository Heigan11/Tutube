package com.tutube.core.controllersTest;

import com.tutube.core.dto.ApiResponse;
import com.tutube.core.dto.User;
import com.tutube.core.dto.UserDto;
import com.tutube.core.repositories.UserRepository;
import com.tutube.core.services.interfaces.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
class UserControllerUpdateTest {

    private static final Logger log = LoggerFactory.getLogger(UserControllerUpdateTest.class);

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @SpyBean
    private UserService userService;

    private UserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        webTestClient = webTestClient.mutate()
                .filter(basicAuthentication("testUser", "password"))
                .build();
        testUserDetails = new org.springframework.security.core.userdetails.User(
                "testUser", "password", Collections.emptyList()
        );
    }

    @Test
    void updateUser_success() {
        log.info("=== Тест: успешное обновление пользователя ===");

        // given - создаем полноценный User объект
        User updateRequest = new User();
        updateRequest.setUserName("testUser");
        updateRequest.setPassword("newPassword123");
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("User");
        updateRequest.setBirthDate(LocalDate.of(1990, 1, 1));
        updateRequest.setLevel(2);
        updateRequest.setSuccessRate(50.0);
        updateRequest.setAttemptsCount(5);

        log.info("1. Подготавливаем полные данные для обновления пользователя: {}", updateRequest.getUsername());

        log.info("1.1. Проверяем наличие пользователя {} в БД", updateRequest.getUsername());
        StepVerifier.create(userRepository.findByUserName(updateRequest.getUsername()))
                .expectNextMatches(user -> {
                    assertThat(user.getUsername()).isEqualTo(updateRequest.getUsername());
                    return true;
                })
                .verifyComplete();

        // when & then
        log.info("2. Отправляем PUT запрос на /api/users");
        webTestClient.put()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(updateRequest))
                .headers(headers -> headers.setBasicAuth("testUser", "password"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<UserDto>>() {})
                .consumeWith(response -> {
                    log.info("3. Статус ответа: 200 OK");
                    ApiResponse<UserDto> apiResponse = response.getResponseBody();
                    assertThat(apiResponse).isNotNull();
                    assertThat(apiResponse.isSuccess()).isTrue();
                    assertThat(apiResponse.getMessage()).isEqualTo("User updated successfully");
                    assertThat(apiResponse.getData().getFirstName()).isEqualTo("Updated");
                    assertThat(apiResponse.getData().getLastName()).isEqualTo("User");
                    assertThat(apiResponse.getData().getLevel()).isEqualTo(2);
                    log.info("4. Пользователь успешно обновлен: {}", apiResponse.getData());
                });

        log.info("5. Проверяем обновление в БД");
        StepVerifier.create(userRepository.findByUserName("testUser"))
                .expectNextMatches(user -> {
                    assertThat(user.getFirstName()).isEqualTo("Updated");
                    assertThat(user.getLastName()).isEqualTo("User");
                    assertThat(user.getLevel()).isEqualTo(2);
                    assertThat(user.getSuccessRate()).isEqualTo(50.0);
                    return true;
                })
                .verifyComplete();

        log.info("=== Тест завершен ===");
    }

    @Test
    void updateUser_accessDenied() {
        log.info("=== Тест: доступ запрещен (обновление чужого пользователя) ===");

        // given
        Map<String, Object> updateRequest = Map.of(
                "userName", "admin", // пытаемся обновить admin
                "firstName", "Hacked"
        );

        log.info("1. Пользователь testUser пытается обновить данные пользователя admin");

        // when & then
        log.info("2. Отправляем PUT запрос");
        webTestClient.put()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody(new ParameterizedTypeReference<ApiResponse<UserDto>>() {})
                .consumeWith(response -> {
                    log.info("3. Статус ответа: 403 Forbidden");
                    ApiResponse<UserDto> apiResponse = response.getResponseBody();
                    assertThat(apiResponse).isNotNull();
                    assertThat(apiResponse.isSuccess()).isFalse();
                    assertThat(apiResponse.getErrorType()).isEqualTo("ACCESS_DENIED");
                    assertThat(apiResponse.getMessage()).isEqualTo("Access denied");
                    log.info("4. Доступ запрещен - тест пройден");
                });

        log.info("=== Тест завершен ===");
    }

    @Test
    void updateUser_validationError() {
        log.info("=== Тест: ошибка валидации (отсутствует username) ===");

        // given
        Map<String, Object> updateRequest = Map.of(
                "firstName", "Test" // нет userName
        );

        log.info("1. Подготавливаем запрос без username");

        // when & then
        log.info("2. Отправляем PUT запрос");
        webTestClient.put()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<ApiResponse<UserDto>>() {})
                .consumeWith(response -> {
                    log.info("3. Статус ответа: 400 Bad Request");
                    ApiResponse<UserDto> apiResponse = response.getResponseBody();
                    assertThat(apiResponse).isNotNull();
                    assertThat(apiResponse.isSuccess()).isFalse();
                    assertThat(apiResponse.getErrorType()).isEqualTo("VALIDATION_ERROR");
                    assertThat(apiResponse.getMessage()).isEqualTo("Username is required");
                    log.info("4. Ошибка валидации - тест пройден");
                });

        log.info("=== Тест завершен ===");
    }

    @Test
    void updateUser_userNotFound() {
        log.info("=== Тест: пользователь не найден ===");

        // given
        Map<String, Object> updateRequest = Map.of(
                "userName", "nonExistentUser",
                "firstName", "Test"
        );

        log.info("1. Пытаемся обновить несуществующего пользователя");

        // when & then
        log.info("2. Отправляем PUT запрос");
        webTestClient.put()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody(new ParameterizedTypeReference<ApiResponse<UserDto>>() {})
                .consumeWith(response -> {
                    log.info("3. Статус ответа: 403 Forbidden");
                    ApiResponse<UserDto> apiResponse = response.getResponseBody();
                    assertThat(apiResponse).isNotNull();
                    assertThat(apiResponse.isSuccess()).isFalse();
                    assertThat(apiResponse.getErrorType()).isEqualTo("ACCESS_DENIED");
                    log.info("4. Пользователь не найден - тест пройден");
                });

        log.info("=== Тест завершен ===");
    }

    @Test
    void updateUser_internalError() {
        log.info("=== Тест: внутренняя ошибка сервера ===");

        // given
        Map<String, Object> updateRequest = Map.of(
                "userName", "testUser",
                "firstName", "Test"
        );

        log.info("1. Мокируем userService для выброса исключения");

        doReturn(Mono.error(new RuntimeException("DB connection failed")))
                .when(userService).updateUser(any(User.class));

        // when & then
        log.info("2. Отправляем PUT запрос");
        webTestClient.put()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(new ParameterizedTypeReference<ApiResponse<UserDto>>() {})
                .consumeWith(response -> {
                    log.info("3. Статус ответа: 500 Internal Server Error");
                    ApiResponse<UserDto> apiResponse = response.getResponseBody();
                    assertThat(apiResponse).isNotNull();
                    assertThat(apiResponse.isSuccess()).isFalse();
                    assertThat(apiResponse.getErrorType()).isEqualTo("INTERNAL_ERROR");
                    assertThat(apiResponse.getMessage()).isEqualTo("Update failed");
                    log.info("4. Внутренняя ошибка - тест пройден");
                });

        log.info("=== Тест завершен ===");
    }
}
