package com.fractal.notify.sms;

import com.fractal.notify.core.NotificationRequest;
import com.fractal.notify.core.NotificationResponse;
import com.fractal.notify.core.NotificationType;
import com.fractal.notify.core.NotificationStrategy;
import com.fractal.notify.sms.dto.SMSRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Strategy implementation for SMS notifications.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SMSNotificationStrategy implements NotificationStrategy {
    private final SMSProviderFactory smsProviderFactory;

    @Override
    public NotificationResponse send(NotificationRequest request) {
        log.debug("Processing SMS notification to {}", request.getTo());

        SMSProvider provider = smsProviderFactory.getProvider();
        // For SMS, use the first recipient (SMS typically sent to one number at a time)
        String recipient = (request.getTo() != null && !request.getTo().isEmpty()) 
                ? request.getTo().get(0) 
                : null;
        
        SMSRequest smsRequest = SMSRequest.builder()
                .to(recipient)
                .from(request.getFrom())
                .body(request.getBody())
                .build();

        SMSResponse smsResponse = provider.sendSMS(smsRequest);

        if (smsResponse.isSuccess()) {
            return NotificationResponse.success(
                    smsResponse.getMessageId(),
                    provider.getProviderName(),
                    NotificationType.SMS
            );
        } else {
            return NotificationResponse.failure(
                    smsResponse.getErrorMessage(),
                    provider.getProviderName(),
                    NotificationType.SMS
            );
        }
    }

    @Override
    public NotificationType getType() {
        return NotificationType.SMS;
    }
}
