package com.tutube.core.configuration;

import com.tutube.core.services.CustomReactiveUserDetailsService;
import com.tutube.core.utils.JwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final String [] publicRoutes = {"/api/auth/**", "/h2-console/**", "/h2-console", "/context/h2-console"};


    private final CustomReactiveUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        System.out.println("====================on filter");
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(publicRoutes).permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterAt(authenticationWebFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .build();
    }

    @Bean
    public AuthenticationWebFilter authenticationWebFilter() {
        AuthenticationWebFilter filter = new AuthenticationWebFilter(authenticationManager());
        filter.setServerAuthenticationConverter(new JwtAuthenticationConverter(jwtUtil));
        return filter;
    }

    @Bean
    public ReactiveAuthenticationManager authenticationManager() {
        return authentication -> {
            // Для JWT аутентификации credentials не нужны
            String username = authentication.getName();

            return userDetailsService.findByUsername(username)
                    .map(userDetails -> new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // credentials = null
                            userDetails.getAuthorities()
                    ));
        };
    }

//    @Bean
//    public ReactiveAuthenticationManager authenticationManager() {
//        UserDetailsRepositoryReactiveAuthenticationManager manager =
//                new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
//        manager.setPasswordEncoder(passwordEncoder());
//        return manager;
//    }

//    @Bean
//    public ReactiveAuthenticationManager authenticationManager() {
//        return new ReactiveAuthenticationManager() {
//            @Override
//            public Mono<Authentication> authenticate(Authentication authentication) {
//
//                System.out.println("=======authentication" + authentication);
//
//                String username = authentication.getName();
//                String password = authentication.getCredentials().toString();
//
//                if (password == null) {
//                    return Mono.error(new BadCredentialsException("Password cannot be null"));
//                }
//
//                return userDetailsService.findByUsername(username)
//                        .flatMap(userDetails -> {
//                            if (passwordEncoder().matches(password, userDetails.getPassword())) {
//                                return Mono.just(new UsernamePasswordAuthenticationToken(
//                                        userDetails, password, userDetails.getAuthorities()));
//                            }
//                            return Mono.error(new BadCredentialsException("Invalid password"));
//                        });
//            }
//        };
//    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
