package com.tutube.core.controllersTest;

import com.tutube.core.dto.ApiResponse;
import com.tutube.core.dto.RegistrationRequest;
import com.tutube.core.repositories.EmailVerificationCodeRepository;
import com.tutube.core.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.springframework.core.ParameterizedTypeReference;
import reactor.test.StepVerifier;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static com.tutube.core.utils.ErrorTypes.*;


@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationCodeRepository emailVerificationCodeRepository;

    @Test
    void register_success_sendsVerificationCode() {
        log.info("=== Тест: успешная регистрация - отправка кода подтверждения ===");

        // given
        String newEmail = "newuser" + System.currentTimeMillis() + "@test.com";
        RegistrationRequest registerRequest = new RegistrationRequest(newEmail);

        log.info("1. Проверяем что пользователя {} нет в БД", newEmail);
        StepVerifier.create(userRepository.findByUserName(newEmail))
                .expectNextCount(0)
                .verifyComplete();

        log.info("3. Отправляем запрос на регистрацию нового пользователя");

        // when & then
        webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<Void>>() {})
                .consumeWith(response -> {
                    log.info("4. Статус ответа: 200 OK");
                    ApiResponse<Void> apiResponse = response.getResponseBody();
                    assertThat(apiResponse).isNotNull();
                    assertThat(apiResponse.isSuccess()).isTrue();
                    assertThat(apiResponse.getMessage()).isEqualTo("Verification code sent to email");
                    log.info("5. Код подтверждения отправлен - тест пройден");
                });

        // Проверяем что пользователь НЕ создан до верификации
        log.info("6. Проверяем что пользователь еще не создан в БД");
        StepVerifier.create(userRepository.findByUserName(newEmail))
                .expectNextCount(0)
                .verifyComplete();

        // НОВАЯ ПРОВЕРКА: проверяем что код сохранен в таблице email_verification_codes
        log.info("7. Проверяем что код подтверждения сохранен в БД");
        StepVerifier.create(emailVerificationCodeRepository.findLatestByEmail(newEmail))
                .expectNextMatches(verificationCode -> {
                    assertThat(verificationCode.getEmail()).isEqualTo(newEmail);
                    assertThat(verificationCode.getCode()).isNotNull();
                    assertThat(verificationCode.isUsed()).isFalse();
                    assertThat(verificationCode.isValid()).isTrue();
                    log.info("8. Код {} правильно сохранен для email {}", verificationCode.getCode(), verificationCode.getEmail());
                    return true;
                })
                .verifyComplete();

        log.info("=== Тест завершен ===");
    }

    @Test
    void register_userAlreadyExists() {
        log.info("=== Тест: регистрация существующего пользователя ===");

        // given - используем существующего пользователя из test-data.sql
        String existingEmail = "testUser@test.com";
        RegistrationRequest registerRequest = new RegistrationRequest(existingEmail);

        log.info("1. Проверяем что пользователь {} существует в БД", existingEmail);
        StepVerifier.create(userRepository.findByUserName(existingEmail))
                .expectNextMatches(user -> {
                    assertThat(user.getUsername()).isEqualTo(existingEmail);
                    return true;
                })
                .verifyComplete();

        log.info("2. Отправляем запрос на регистрацию существующего пользователя");

        // when & then
        webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody(new ParameterizedTypeReference<ApiResponse<Void>>() {})
                .consumeWith(response -> {
                    log.info("3. Статус ответа: 409 Conflict");
                    ApiResponse<Void> apiResponse = response.getResponseBody();
                    assertThat(apiResponse).isNotNull();
                    assertThat(apiResponse.isSuccess()).isFalse();
                    assertThat(apiResponse.getErrorType()).isEqualTo(USER_ALREADY_EXISTS);
                    assertThat(apiResponse.getMessage()).isEqualTo("User already exists");
                    log.info("4. Пользователь уже существует - тест пройден");
                });

        log.info("=== Тест завершен ===");
    }

    @Test
    void register_invalidEmailFormat() {
        log.info("=== Тест: регистрация с некорректным email ===");

        // given
        String invalidEmail = "invalid-email";
        RegistrationRequest registerRequest = new RegistrationRequest(invalidEmail);

        log.info("1. Отправляем запрос с некорректным email: {}", invalidEmail);

        // when & then
        webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<ApiResponse<Void>>() {})
                .consumeWith(response -> {
                    log.info("2. Статус ответа: 400 Bad Request");
                    ApiResponse<Void> apiResponse = response.getResponseBody();
                    assertThat(apiResponse).isNotNull();
                    assertThat(apiResponse.isSuccess()).isFalse();
                    assertThat(apiResponse.getErrorType()).isEqualTo(VALIDATION_ERROR);
                    log.info("3. Некорректный email - тест пройден");
                });

        log.info("=== Тест завершен ===");
    }


    @Test
    void register_resendProtection() {
        log.info("=== Тест: защита от частой повторной отправки кода ===");

        // given
        String newEmail = "newuser" + System.currentTimeMillis() + "@test.com";
        RegistrationRequest registerRequest = new RegistrationRequest(newEmail);

        log.info("1. Первая регистрация - отправка кода");
        webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isOk();

        log.info("2. Немедленная повторная регистрация с тем же email");
        webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isBadRequest() // или тот статус, который возвращает твой сервис
                .expectBody(new ParameterizedTypeReference<ApiResponse<Void>>() {})
                .consumeWith(response -> {
                    log.info("3. Статус ответа: 400 Bad Request");
                    ApiResponse<Void> apiResponse = response.getResponseBody();
                    assertThat(apiResponse).isNotNull();
                    assertThat(apiResponse.isSuccess()).isFalse();
                    assertThat(apiResponse.getMessage()).startsWith("Повторная отправка кода возможна только через ");
                    assertThat(apiResponse.getErrorType()).isEqualTo(VALIDATION_ERROR);
                    log.info("4. Защита от частых запросов работает - тест пройден");
                });

        log.info("=== Тест завершен ===");
    }
}
