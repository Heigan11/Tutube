package com.tutube.core.services.interfaces;

import reactor.core.publisher.Mono;

public interface EmailVerificationService {
    Mono<String> generateAndSendVerificationCode(String email);
    Mono<Boolean> verifyCode(String email, String code);
    Mono<Boolean> canResendCode(String email);
}
