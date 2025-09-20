package com.tutube.core.controllersTest;

import com.tutube.core.configuration.JwtUtil;
import com.tutube.core.configuration.TestSecurityConfig;
import com.tutube.core.conrollers.AuthController;
import com.tutube.core.dto.ApiResponse;
import com.tutube.core.dto.User;
import com.tutube.core.dto.UserDto;
import com.tutube.core.repositories.UserRepository;
import com.tutube.core.services.interfaces.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static com.tutube.core.utils.ErrorTypes.USER_ALREADY_EXISTS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = AuthController.class)
@Import({JwtUtil.class, TestSecurityConfig.class}) // Импортируем тестовую конфигурацию
@ActiveProfiles("test") // Активируем профиль "test"
class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    private User testUser;
    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "encodedPassword");
        testUser.setId(1L);
        testUserDto = UserDto.convertToUserDto(testUser);

        when(jwtUtil.generateToken(anyString())).thenReturn("test-jwt-token");
    }

    @Disabled
    @Test
    void register_WhenUserDoesNotExist_ShouldReturnCreated() {
        AuthController.RegistrationRequest request = new AuthController.RegistrationRequest();
        request.setUserName("newuser");
        request.setPassword("password123");

        when(userRepository.findByUserName("newuser")).thenReturn(Mono.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));


        webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();

    }
}
