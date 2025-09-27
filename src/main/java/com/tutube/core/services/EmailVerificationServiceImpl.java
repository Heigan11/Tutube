package com.tutube.core.services;

import com.tutube.core.entity.EmailVerificationCode;
import com.tutube.core.repositories.EmailVerificationCodeRepository;
import com.tutube.core.services.interfaces.EmailVerificationService;
import freemarker.template.TemplateException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import freemarker.template.Configuration;
import freemarker.template.Template;


@Service
@Slf4j
@RequiredArgsConstructor
@AllArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final EmailVerificationCodeRepository codeRepository;
    private final JavaMailSender mailSender;
    private final Configuration freeMarkerConfig;


    @Value("${app.email.verification.code-expiration-minutes:10}")
    private int codeExpirationMinutes;

    @Value("${app.email.verification.resend-timeout-minutes:3}")
    private int resendTimeoutMinutes;

    @Override
    public Mono<String> generateAndSendVerificationCode(String email) {
        return canResendCode(email)
                .flatMap(canResend -> {
                    if (!canResend) {
                        return Mono.error(new RuntimeException("Повторная отправка кода возможна только через " + resendTimeoutMinutes + " минуты"));
                    }

                    String code = generateRandomCode();
                    EmailVerificationCode verificationCode =
                            new EmailVerificationCode(email, code, codeExpirationMinutes);

                    return codeRepository.save(verificationCode)
                            .doOnSuccess(savedCode -> sendVerificationEmail(email, code))
                            .map(EmailVerificationCode::getCode);
                });
    }

    @Override
    public Mono<Boolean> verifyCode(String email, String code) {
        return codeRepository.findByEmailAndCode(email, code)
                .flatMap(verificationCode -> {
                    if (!verificationCode.isValid()) {
                        return Mono.just(false);
                    }

                    verificationCode.setUsed(true);
                    return codeRepository.save(verificationCode)
                            .thenReturn(true);
                })
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Boolean> canResendCode(String email) {
        return codeRepository.findLatestByEmail(email)
                .map(latestCode -> {
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime minResendTime = latestCode.getCreatedAt().plusMinutes(resendTimeoutMinutes);
                    return now.isAfter(minResendTime);
                })
                .defaultIfEmpty(true); // Если кодов еще не было - можно отправлять
    }

    private String generateRandomCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(999999));
    }

    private void sendVerificationEmail(String email, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("Подтверждение регистрации");
            helper.setText(buildEmailContent(code), true);

            mailSender.send(message);
            log.info("Код подтверждения отправлен на email: {}", email);
        } catch (Exception e) {
            log.error("Ошибка отправки email на {}: {}", email, e.getMessage());
        }
    }

    private String buildEmailContent(String code) throws TemplateException, IOException {
        Map<String, Object> model = new HashMap<>();
        model.put("code", code);
        model.put("expirationMinutes", codeExpirationMinutes);

        Template template = freeMarkerConfig.getTemplate("email-verification.ftl");
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
    }
}
