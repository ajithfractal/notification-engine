package com.fractal.notify.email.provider;

import com.fractal.notify.config.NotificationProperties;
import com.fractal.notify.email.EmailProvider;
import com.fractal.notify.email.EmailResponse;
import com.fractal.notify.email.dto.EmailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;

/**
 * SMTP implementation of EmailProvider using Spring Mail.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SMTPEmailProvider implements EmailProvider {
    private final JavaMailSender mailSender;
    private final NotificationProperties properties;
    private static final String PROVIDER_NAME = "smtp";

    @Override
    public EmailResponse sendEmail(EmailRequest request) {
        if (mailSender == null) {
            log.error("JavaMailSender not configured. Please configure SMTP settings.");
            return EmailResponse.builder()
                    .success(false)
                    .errorMessage("SMTP not configured")
                    .build();
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(request.getTo());
            helper.setFrom(request.getFrom() != null ? request.getFrom() : getDefaultFrom());
            helper.setSubject(request.getSubject());
            helper.setText(request.getBody(), true); // true indicates HTML

            mailSender.send(message);

            log.info("Email sent successfully via SMTP to {}", request.getTo());
            return EmailResponse.builder()
                    .success(true)
                    .messageId("smtp-" + System.currentTimeMillis())
                    .build();
        } catch (Exception e) {
            log.error("Error sending email via SMTP", e);
            return EmailResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    private String getDefaultFrom() {
        if (properties != null && properties.getEmail().getSmtp().getFrom() != null) {
            return properties.getEmail().getSmtp().getFrom();
        }
        return "noreply@fractal.com";
    }
}
