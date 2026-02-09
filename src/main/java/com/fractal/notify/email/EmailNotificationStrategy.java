package com.fractal.notify.email;

import com.fractal.notify.core.NotificationRequest;
import com.fractal.notify.core.NotificationResponse;
import com.fractal.notify.core.NotificationType;
import com.fractal.notify.core.NotificationStrategy;
import com.fractal.notify.email.dto.EmailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Strategy implementation for email notifications.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationStrategy implements NotificationStrategy {
    private final EmailProviderFactory emailProviderFactory;

    @Override
    public NotificationResponse send(NotificationRequest request) {
        log.debug("Processing email notification to {}", request.getTo() != null ? request.getTo() : "N/A");

        EmailProvider provider = emailProviderFactory.getProvider();
        EmailRequest emailRequest = EmailRequest.builder()
                .to(request.getTo())
                .cc(request.getCc())
                .bcc(request.getBcc())
                .from(request.getFrom())
                .replyTo(request.getReplyTo())
                .subject(request.getSubject())
                .body(request.getBody())
                .attachments(request.getAttachments())
                .build();

        EmailResponse emailResponse = provider.sendEmail(emailRequest);

        if (emailResponse.isSuccess()) {
            return NotificationResponse.success(
                    emailResponse.getMessageId(),
                    provider.getProviderName(),
                    NotificationType.EMAIL
            );
        } else {
            return NotificationResponse.failure(
                    emailResponse.getErrorMessage(),
                    provider.getProviderName(),
                    NotificationType.EMAIL
            );
        }
    }

    @Override
    public NotificationType getType() {
        return NotificationType.EMAIL;
    }
}
