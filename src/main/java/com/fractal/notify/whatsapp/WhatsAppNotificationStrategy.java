package com.fractal.notify.whatsapp;

import com.fractal.notify.core.NotificationRequest;
import com.fractal.notify.core.NotificationResponse;
import com.fractal.notify.core.NotificationType;
import com.fractal.notify.core.NotificationStrategy;
import com.fractal.notify.whatsapp.dto.WhatsAppRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Strategy implementation for WhatsApp notifications.
 * Currently a stub implementation for future development.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WhatsAppNotificationStrategy implements NotificationStrategy {
    private final WhatsAppProviderFactory whatsAppProviderFactory;

    @Override
    public NotificationResponse send(NotificationRequest request) {
        log.debug("Processing WhatsApp notification to {}", request.getTo());

        WhatsAppProvider provider = whatsAppProviderFactory.getProvider();
        // For WhatsApp, use the first recipient (WhatsApp typically sent to one number at a time)
        String recipient = (request.getTo() != null && !request.getTo().isEmpty()) 
                ? request.getTo().get(0) 
                : null;
        
        WhatsAppRequest whatsAppRequest = WhatsAppRequest.builder()
                .to(recipient)
                .from(request.getFrom())
                .body(request.getBody())
                .build();

        WhatsAppResponse whatsAppResponse = provider.sendWhatsApp(whatsAppRequest);

        if (whatsAppResponse.isSuccess()) {
            return NotificationResponse.success(
                    whatsAppResponse.getMessageId(),
                    provider.getProviderName(),
                    NotificationType.WHATSAPP
            );
        } else {
            return NotificationResponse.failure(
                    whatsAppResponse.getErrorMessage(),
                    provider.getProviderName(),
                    NotificationType.WHATSAPP
            );
        }
    }

    @Override
    public NotificationType getType() {
        return NotificationType.WHATSAPP;
    }
}
