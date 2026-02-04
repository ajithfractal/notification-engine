package com.fractal.notify.core;

import com.fractal.notify.async.AsyncNotificationPublisher;
import com.fractal.notify.template.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Main service facade for sending notifications.
 * Routes requests to appropriate strategies and handles async processing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final List<NotificationStrategy> strategies;
    private final TemplateService templateService;
    private final AsyncNotificationPublisher asyncPublisher; // Injected based on configuration

    /**
     * Send a notification asynchronously.
     * Uses the configured AsyncNotificationPublisher (currently @Async, can be Kafka in future).
     *
     * @param request the notification request
     * @return CompletableFuture with the notification response
     */
    public CompletableFuture<NotificationResponse> sendAsync(NotificationRequest request) {
        log.debug("Publishing notification via {}", asyncPublisher.getPublisherType());
        return asyncPublisher.publishAsync(request);
    }

    /**
     * Send a notification synchronously.
     *
     * @param request the notification request
     * @return the notification response
     */
    public NotificationResponse send(NotificationRequest request) {
        log.info("Sending {} notification to {}", request.getNotificationType(), request.getTo());

        // Render template if template name is provided and body is not already set
        if (request.getTemplateName() != null && !request.getTemplateName().isEmpty() 
                && (request.getBody() == null || request.getBody().isEmpty())) {
            String renderedContent = templateService.renderTemplate(
                    request.getNotificationType(),
                    request.getTemplateName(),
                    request.getTemplateVariables() != null ? request.getTemplateVariables() : Map.of()
            );
            request = NotificationRequest.builder()
                    .notificationType(request.getNotificationType())
                    .to(request.getTo())
                    .subject(request.getSubject())
                    .body(renderedContent)
                    .templateName(request.getTemplateName())
                    .templateVariables(request.getTemplateVariables())
                    .from(request.getFrom())
                    .build();
        }

        // Find appropriate strategy
        NotificationStrategy strategy = findStrategy(request.getNotificationType());
        if (strategy == null) {
            log.error("No strategy found for notification type: {}", request.getNotificationType());
            return NotificationResponse.failure(
                    "No strategy found for notification type: " + request.getNotificationType(),
                    "N/A",
                    request.getNotificationType()
            );
        }

        // Send notification using the strategy
        try {
            NotificationResponse response = strategy.send(request);
            log.info("Notification sent successfully. Provider: {}, MessageId: {}", 
                    response.getProvider(), response.getMessageId());
            return response;
        } catch (Exception e) {
            log.error("Error sending notification", e);
            return NotificationResponse.failure(
                    e.getMessage(),
                    "N/A",
                    request.getNotificationType()
            );
        }
    }

    /**
     * Find the appropriate strategy for the given notification type.
     *
     * @param type the notification type
     * @return the strategy or null if not found
     */
    private NotificationStrategy findStrategy(NotificationType type) {
        return strategies.stream()
                .filter(strategy -> strategy.getType() == type)
                .findFirst()
                .orElse(null);
    }
}
