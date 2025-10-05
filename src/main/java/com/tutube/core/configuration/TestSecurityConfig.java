package com.tutube.core.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;

//@Configuration
//@EnableWebFluxSecurity
//@Profile("test")
//@EnableReactiveMethodSecurity


@Configuration
@EnableWebFluxSecurity
@Profile("test")
public class TestSecurityConfig {

//    @Bean
//    @Primary
//    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
//        return http
//                .csrf(ServerHttpSecurity.CsrfSpec::disable)
//                .authorizeExchange(exchanges -> exchanges
//                        .anyExchange().permitAll()
//                )
//                .build();
//    }

    @Bean
    @Primary
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/auth/register", "/api/auth/verify").permitAll()
                        .anyExchange().authenticated()  // ✅ Требуем аутентификацию
                )
                .httpBasic(Customizer.withDefaults())  // ✅ Включаем HTTP Basic
                .build();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        // ✅ Создаем тестового пользователя в памяти
        UserDetails user = User.withUsername("testUser")
                .password("{noop}password")  // {noop} для простого пароля
                .roles("USER")
                .build();

        UserDetails admin = User.withUsername("admin")
                .password("{noop}password")
                .roles("ADMIN")
                .build();

        return new MapReactiveUserDetailsService(user, admin);
    }
}



//         .csrf(ServerHttpSecurity.CsrfSpec::disable) /
