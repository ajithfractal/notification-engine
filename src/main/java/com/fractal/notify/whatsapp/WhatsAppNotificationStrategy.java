package com.fractal.notify.whatsapp;

import com.fractal.notify.core.NotificationRequest;
import com.fractal.notify.core.NotificationResponse;
import com.fractal.notify.core.NotificationType;
import com.fractal.notify.core.NotificationStrategy;
import com.fractal.notify.template.TemplateService;
import com.fractal.notify.whatsapp.dto.WhatsAppRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Strategy implementation for WhatsApp notifications.
 * Currently a stub implementation for future development.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WhatsAppNotificationStrategy implements NotificationStrategy {
    private final WhatsAppProviderFactory whatsAppProviderFactory;
    private final TemplateService templateService;

    @Override
    public NotificationResponse send(NotificationRequest request) {
        log.debug("Processing WhatsApp notification to {}", request.getTo());

        WhatsAppProvider provider = whatsAppProviderFactory.getProvider();
        if (provider == null) {
            log.error("WhatsApp provider not found. Please configure a WhatsApp provider.");
            return NotificationResponse.failure(
                    "WhatsApp provider not configured",
                    "N/A",
                    NotificationType.WHATSAPP
            );
        }

        // For WhatsApp, use the first recipient (WhatsApp typically sent to one number at a time)
        String recipient = (request.getTo() != null && !request.getTo().isEmpty()) 
                ? request.getTo().get(0) 
                : null;
        
        if (recipient == null || recipient.trim().isEmpty()) {
            log.error("WhatsApp recipient is required");
            return NotificationResponse.failure(
                    "WhatsApp recipient is required",
                    provider.getProviderName(),
                    NotificationType.WHATSAPP
            );
        }
        
        // Check if using Twilio Content Template (content_sid)
        String contentSid = null;
        Map<String, String> contentVariables = null;
        
        if (request.getTemplateName() != null && !request.getTemplateName().isEmpty()) {
            try {
                var template = templateService.getTemplate(NotificationType.WHATSAPP, request.getTemplateName());
                if (template.getContentSid() != null && !template.getContentSid().trim().isEmpty()) {
                    // Use Twilio Content Template
                    contentSid = template.getContentSid();
                    // Convert template variables to String map for Twilio Content API
                    if (request.getTemplateVariables() != null && !request.getTemplateVariables().isEmpty()) {
                        contentVariables = request.getTemplateVariables().entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> entry.getValue() != null ? entry.getValue().toString() : ""
                                ));
                    }
                    log.debug("Using Twilio Content Template: contentSid={}", contentSid);
                }
            } catch (Exception e) {
                log.warn("Could not get template metadata for WhatsApp Content Template, using body instead: {}", e.getMessage());
            }
        }
        
        WhatsAppRequest.WhatsAppRequestBuilder requestBuilder = WhatsAppRequest.builder()
                .to(recipient)
                .from(request.getFrom());
        
        if (contentSid != null) {
            // Use Content Template
            requestBuilder.contentSid(contentSid)
                    .contentVariables(contentVariables);
        } else {
            // Use regular body
            requestBuilder.body(request.getBody());
        }
        
        WhatsAppRequest whatsAppRequest = requestBuilder.build();

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
