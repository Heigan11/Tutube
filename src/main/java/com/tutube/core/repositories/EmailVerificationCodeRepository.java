package com.tutube.core.repositories;

import com.tutube.core.entity.EmailVerificationCode;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface EmailVerificationCodeRepository extends R2dbcRepository<EmailVerificationCode, Long> {

    @Query("SELECT * FROM email_verification_codes WHERE email = :email AND code = :code ORDER BY created_at DESC LIMIT 1")
    Mono<EmailVerificationCode> findByEmailAndCode(String email, String code);

    @Query("SELECT * FROM email_verification_codes WHERE email = :email ORDER BY created_at DESC LIMIT 1")
    Mono<EmailVerificationCode> findLatestByEmail(String email);
}
