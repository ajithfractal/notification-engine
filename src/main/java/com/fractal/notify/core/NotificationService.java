package com.fractal.notify.core;

import com.fractal.notify.async.AsyncNotificationPublisher;
import com.fractal.notify.config.NotificationProperties;
import com.fractal.notify.persistence.NotificationPersistenceService;
import com.fractal.notify.persistence.entity.NotificationEntity;
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
    private final NotificationPersistenceService persistenceService;
    private final NotificationProperties properties;

    /**
     * Send a notification asynchronously.
     * Uses the configured AsyncNotificationPublisher (currently @Async, can be Kafka in future).
     * Persists notification to database before sending if persistence is enabled.
     *
     * @param request the notification request
     * @return CompletableFuture with the notification response
     */
    public CompletableFuture<NotificationResponse> sendAsync(NotificationRequest request) {
        log.debug("Publishing notification via {}", asyncPublisher.getPublisherType());
        
        // Persist to database before sending if persistence is enabled
        NotificationEntity entity = null;
        if (properties.getPersistence() != null && properties.getPersistence().isEnabled()) {
            try {
                entity = persistenceService.persistBeforeSend(request);
            } catch (Exception e) {
                log.error("Failed to persist notification before sending", e);
                // Continue with sending even if persistence fails
            }
        }
        
        final NotificationEntity finalEntity = entity;
        CompletableFuture<NotificationResponse> future = asyncPublisher.publishAsync(request);
        
        // Update persistence after sending
        if (finalEntity != null) {
            future.whenComplete((response, throwable) -> {
                if (throwable == null) {
                    try {
                        String provider = response != null ? response.getProvider() : "unknown";
                        persistenceService.updateAfterSend(finalEntity.getId(), response, provider);
                    } catch (Exception e) {
                        log.error("Failed to update notification status after sending", e);
                    }
                } else {
                    // Handle exception case
                    try {
                        NotificationResponse errorResponse = NotificationResponse.failure(
                                throwable.getMessage(),
                                "unknown",
                                request.getNotificationType()
                        );
                        persistenceService.updateAfterSend(finalEntity.getId(), errorResponse, "unknown");
                    } catch (Exception e) {
                        log.error("Failed to update notification status after error", e);
                    }
                }
            });
        }
        
        return future;
    }

    /**
     * Send a notification synchronously.
     *
     * @param request the notification request
     * @return the notification response
     */
    public NotificationResponse send(NotificationRequest request) {
        log.info("Sending {} notification to {}", request.getNotificationType(), request.getTo() != null ? request.getTo() : "N/A");

        // Render template if template name or content is provided and body is not already set
        if ((request.getTemplateName() != null && !request.getTemplateName().isEmpty()) 
                || (request.getTemplateContent() != null && !request.getTemplateContent().isEmpty())) {
            if (request.getBody() == null || request.getBody().isEmpty()) {
                String renderedContent = templateService.renderTemplate(
                        request.getNotificationType(),
                        request.getTemplateName(),
                        request.getTemplateContent(),
                        request.getTemplateVariables() != null ? request.getTemplateVariables() : Map.of()
                );
                request = NotificationRequest.builder()
                        .notificationType(request.getNotificationType())
                        .to(request.getTo())
                        .cc(request.getCc())
                        .bcc(request.getBcc())
                        .subject(request.getSubject())
                        .body(renderedContent)
                        .templateName(request.getTemplateName())
                        .templateContent(request.getTemplateContent())
                        .templateVariables(request.getTemplateVariables())
                        .from(request.getFrom())
                        .build();
            }
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
