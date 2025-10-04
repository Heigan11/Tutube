package com.tutube.core.controllersTest;

import com.tutube.core.dto.ApiResponse;
import com.tutube.core.dto.EmailVerificationRequest;
import com.tutube.core.dto.RegistrationRequest;
import com.tutube.core.dto.UserDto;
import com.tutube.core.entity.EmailVerificationCode;
import com.tutube.core.repositories.EmailVerificationCodeRepository;
import com.tutube.core.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.springframework.core.ParameterizedTypeReference;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static com.tutube.core.utils.ErrorTypes.*;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AuthControllerVerificationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationCodeRepository emailVerificationCodeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void verifyEmail_success_createsUser() {
        log.info("=== Тест: успешная верификация - создание пользователя ===");

        // given
        String newEmail = "verifyuser" + System.currentTimeMillis() + "@test.com";

        // 1. Сначала регистрируем пользователя (создаем код в БД)
        log.info("1. Регистрируем пользователя для получения кода подтверждения");
        RegistrationRequest registerRequest = new RegistrationRequest(newEmail);
        webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isOk();

        // 2. Получаем код из БД
        log.info("2. Получаем код подтверждения из БД");
        EmailVerificationCode verificationCode = emailVerificationCodeRepository
                .findLatestByEmail(newEmail)
                .block(); // используем block() только в тестах

        assertThat(verificationCode).isNotNull();
        String code = verificationCode.getCode();
        log.info("3. Получен код: {} для email: {}", code, newEmail);

        EmailVerificationRequest verifyRequest = new EmailVerificationRequest(newEmail, code);

        log.info("4. Отправляем запрос на верификацию email");

        // when & then
        webTestClient.post()
                .uri("/api/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(verifyRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<UserDto>>() {})
                .consumeWith(response -> {
                    log.info("5. Статус ответа: 201 Created");
                    ApiResponse<UserDto> apiResponse = response.getResponseBody();
                    assertThat(apiResponse).isNotNull();
                    assertThat(apiResponse.isSuccess()).isTrue();
                    assertThat(apiResponse.getMessage()).isEqualTo("User registered successfully");
                    assertThat(apiResponse.getData().getUserName()).isEqualTo(newEmail);
                    assertThat(apiResponse.getToken()).isNotNull();
                    log.info("6. Пользователь успешно создан - тест пройден");
                });

        // Проверяем что пользователь создан в БД
        log.info("7. Проверяем что пользователь создан в БД");
        StepVerifier.create(userRepository.findByUserName(newEmail))
                .expectNextMatches(user -> {
                    assertThat(user.getUsername()).isEqualTo(newEmail);
                    assertThat(passwordEncoder.matches(code, user.getPassword())).isTrue();
                    log.info("8. Пользователь создан с кодом как паролем");
                    return true;
                })
                .verifyComplete();

        // Проверяем что код помечен как использованный
        log.info("9. Проверяем что код подтверждения помечен как использованный");
        StepVerifier.create(emailVerificationCodeRepository.findLatestByEmail(newEmail))
                .expectNextMatches(usedCode -> {
                    assertThat(usedCode.isUsed()).isTrue();
                    log.info("10. Код помечен как использованный");
                    return true;
                })
                .verifyComplete();

        log.info("=== Тест завершен ===");
    }

    @Test
    void verifyEmail_invalidCode() {
        log.info("=== Тест: верификация с неверным кодом ===");

        // given
        String email = "testuser" + System.currentTimeMillis() + "@test.com";

        // Сначала регистрируем чтобы был код в БД
        log.info("1. Регистрируем пользователя");
        RegistrationRequest registerRequest = new RegistrationRequest(email);
        webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isOk();

        // Используем неверный код
        String wrongCode = "999999";
        EmailVerificationRequest verifyRequest = new EmailVerificationRequest(email, wrongCode);

        log.info("2. Отправляем запрос на верификацию с неверным кодом");

        // when & then
        webTestClient.post()
                .uri("/api/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(verifyRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<ApiResponse<UserDto>>() {})
                .consumeWith(response -> {
                    log.info("3. Статус ответа: 400 Bad Request");
                    ApiResponse<UserDto> apiResponse = response.getResponseBody();
                    assertThat(apiResponse).isNotNull();
                    assertThat(apiResponse.isSuccess()).isFalse();
                    assertThat(apiResponse.getErrorType()).isEqualTo(VALIDATION_ERROR);
                    assertThat(apiResponse.getMessage()).isEqualTo("Invalid or expired code");
                    log.info("4. Неверный код отклонен - тест пройден");
                });

        // Проверяем что пользователь НЕ создан
        log.info("5. Проверяем что пользователь не создан в БД");
        StepVerifier.create(userRepository.findByUserName(email))
                .expectNextCount(0)
                .verifyComplete();

        log.info("=== Тест завершен ===");
    }

    @Test
    void verifyEmail_expiredCode() {
        log.info("=== Тест: верификация с просроченным кодом ===");

        // given
        String email = "expireduser" + System.currentTimeMillis() + "@test.com";

        // Создаем код вручную с истекшим сроком
        log.info("1. Создаем просроченный код в БД");
        EmailVerificationCode expiredCode = new EmailVerificationCode();
        expiredCode.setEmail(email);
        expiredCode.setCode("123456");
        expiredCode.setCreatedAt(LocalDateTime.now().minusHours(1)); // код создан час назад
        expiredCode.setExpiresAt(LocalDateTime.now().minusMinutes(50)); // истек 50 минут назад
        expiredCode.setUsed(false);

        emailVerificationCodeRepository.save(expiredCode).block();

        EmailVerificationRequest verifyRequest = new EmailVerificationRequest(email, "123456");

        log.info("2. Отправляем запрос на верификацию с просроченным кодом");

        // when & then
        webTestClient.post()
                .uri("/api/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(verifyRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<ApiResponse<UserDto>>() {})
                .consumeWith(response -> {
                    log.info("3. Статус ответа: 400 Bad Request");
                    ApiResponse<UserDto> apiResponse = response.getResponseBody();
                    assertThat(apiResponse).isNotNull();
                    assertThat(apiResponse.isSuccess()).isFalse();
                    assertThat(apiResponse.getErrorType()).isEqualTo(VALIDATION_ERROR);
                    assertThat(apiResponse.getMessage()).isEqualTo("Invalid or expired code");
                    log.info("4. Просроченный код отклонен - тест пройден");
                });

        log.info("5. Проверяем что пользователь не создан в БД");
        StepVerifier.create(userRepository.findByUserName(email))
                .expectNextCount(0)
                .verifyComplete();

        log.info("=== Тест завершен ===");
    }

    @Test
    void verifyEmail_alreadyUsedCode() {
        log.info("=== Тест: верификация с уже использованным кодом ===");

        // given
        String email = "usedcodeuser" + System.currentTimeMillis() + "@test.com";

        // Создаем уже использованный код
        log.info("1. Создаем использованный код в БД");
        EmailVerificationCode usedCode = new EmailVerificationCode();
        usedCode.setEmail(email);
        usedCode.setCode("123456");
        usedCode.setCreatedAt(LocalDateTime.now());
        usedCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        usedCode.setUsed(true); // уже использован

        emailVerificationCodeRepository.save(usedCode).block();

        EmailVerificationRequest verifyRequest = new EmailVerificationRequest(email, "123456");

        log.info("2. Отправляем запрос на верификацию с использованным кодом");

        // when & then
        webTestClient.post()
                .uri("/api/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(verifyRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<ApiResponse<UserDto>>() {})
                .consumeWith(response -> {
                    log.info("3. Статус ответа: 400 Bad Request");
                    ApiResponse<UserDto> apiResponse = response.getResponseBody();
                    assertThat(apiResponse).isNotNull();
                    assertThat(apiResponse.isSuccess()).isFalse();
                    assertThat(apiResponse.getErrorType()).isEqualTo(VALIDATION_ERROR);
                    assertThat(apiResponse.getMessage()).isEqualTo("Invalid or expired code");
                    log.info("4. Использованный код отклонен - тест пройден");
                });

        log.info("=== Тест завершен ===");
    }
}
