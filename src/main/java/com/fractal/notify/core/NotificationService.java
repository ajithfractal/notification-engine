package com.fractal.notify.core;

import com.fractal.notify.async.AsyncNotificationPublisher;
import com.fractal.notify.config.NotificationProperties;
import com.fractal.notify.persistence.NotificationPersistenceService;
import com.fractal.notify.persistence.entity.NotificationEntity;
import com.fractal.notify.rabbitmq.RabbitMQNotificationPublisher;
import com.fractal.notify.rabbitmq.dto.NotificationMessage;
import com.fractal.notify.staticasset.StaticAssetService;
import com.fractal.notify.template.TemplateNotFoundException;
import com.fractal.notify.template.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final NotificationProperties properties;
    
    @Autowired(required = false)
    private NotificationPersistenceService persistenceService;
    
    @Autowired(required = false)
    private RabbitMQNotificationPublisher rabbitMQPublisher;
    
    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;
    
    @Autowired(required = false)
    private StaticAssetService staticAssetService;

    /**
     * Send a notification asynchronously.
     * Validates template BEFORE persisting to database.
     * If queue mode is enabled, persists to database and returns immediately (scheduler will process).
     * Otherwise, uses the configured AsyncNotificationPublisher (default is @Async, can be RabbitMQ or others).
     *
     * @param request the notification request
     * @return CompletableFuture with the notification response
     */
    public CompletableFuture<NotificationResponse> sendAsync(NotificationRequest request) {
        // Validate template FIRST (before persisting)
        if (request.getTemplateName() != null && !request.getTemplateName().isEmpty()) {
            try {
                // Validate template exists and is active (don't render yet, just validate)
                validateTemplate(request.getTemplateName(), request.getNotificationType());
            } catch (TemplateNotFoundException e) {
                log.error("Template validation failed: {}", e.getMessage());
                return CompletableFuture.completedFuture(
                        NotificationResponse.failure(
                                e.getMessage(),
                                "template-validation",
                                request.getNotificationType()
                        )
                );
            }
        }

        // If queue mode is enabled, persist and return
        if (isQueueModeEnabled()) {
            log.debug("Queue mode enabled, persisting notification to queue");
            try {
                NotificationEntity entity = persistenceService.persistBeforeSend(request);
                log.info("Notification queued with ID: {}", entity.getId());
                
                return CompletableFuture.completedFuture(
                        NotificationResponse.builder()
                                .success(true)
                                .messageId("QUEUED-" + entity.getId())
                                .provider("queue")
                                .notificationType(request.getNotificationType())
                                .message("Notification queued successfully")
                                .build()
                );
            } catch (Exception e) {
                log.error("Failed to persist notification to queue", e);
                return CompletableFuture.completedFuture(
                        NotificationResponse.failure(
                                "Failed to queue notification: " + e.getMessage(),
                                "queue",
                                request.getNotificationType()
                        )
                );
            }
        }
        
        // Check if RabbitMQ mode is enabled
        if (isRabbitMQModeEnabled()) {
            return publishToRabbitMQ(request);
        }
        
        // Queue mode disabled - use async publisher (default @Async)
        log.debug("Publishing notification via {}", asyncPublisher.getPublisherType());
        
        // Conditionally persist to database before sending
        NotificationEntity entity = null;
        if (isPersistenceEnabled()) {
            try {
                entity = persistenceService.persistBeforeSend(request);
                log.debug("Notification persisted with ID: {}", entity != null ? entity.getId() : "N/A");
            } catch (Exception e) {
                log.error("Failed to persist notification before sending", e);
                // Continue with sending even if persistence fails
            }
        } else {
            log.debug("Persistence is disabled, skipping database persistence");
        }
        
        final NotificationEntity finalEntity = entity;
        CompletableFuture<NotificationResponse> future = asyncPublisher.publishAsync(request);
        
        // Conditionally update persistence after sending (only for non-RabbitMQ mode)
        if (isPersistenceEnabled() && finalEntity != null) {
            future.whenComplete((response, throwable) -> {
                if (throwable == null) {
                    try {
                        String provider = response != null ? response.getProvider() : "unknown";
                        persistenceService.updateAfterSend(finalEntity.getId(), response, provider);
                        log.debug("Notification status updated in database: {}", finalEntity.getId());
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
                        log.debug("Notification error status updated in database: {}", finalEntity.getId());
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
     * Validates template BEFORE processing.
     *
     * @param request the notification request
     * @return the notification response
     */
    public NotificationResponse send(NotificationRequest request) {
        log.info("Sending {} notification to {}", request.getNotificationType(), request.getTo() != null ? request.getTo() : "N/A");

        // Validate and render template if template name is provided
        if (request.getTemplateName() != null && !request.getTemplateName().isEmpty()) {
            if (request.getBody() == null || request.getBody().isEmpty()) {
                try {
                    String renderedContent = templateService.renderTemplate(
                            request.getNotificationType(),
                            request.getTemplateName(),
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
                            .templateVariables(request.getTemplateVariables())
                            .from(request.getFrom())
                            .replyTo(request.getReplyTo())
                            .attachments(request.getAttachments())
                            .build();
                } catch (TemplateNotFoundException e) {
                    log.error("Template not found: {}", e.getMessage());
                    return NotificationResponse.failure(
                            e.getMessage(),
                            "template-validation",
                            request.getNotificationType()
                    );
                }
            }
        }

        // Replace static asset references (cid:) with URLs for email notifications
        if (request.getNotificationType() == NotificationType.EMAIL && request.getBody() != null && staticAssetService != null) {
            try {
                String bodyWithUrls = staticAssetService.replaceStaticAssetReferences(request.getBody());
                if (!bodyWithUrls.equals(request.getBody())) {
                    request = NotificationRequest.builder()
                            .notificationType(request.getNotificationType())
                            .to(request.getTo())
                            .cc(request.getCc())
                            .bcc(request.getBcc())
                            .subject(request.getSubject())
                            .body(bodyWithUrls) // Body now has URLs instead of cid:
                            .templateName(request.getTemplateName())
                            .templateVariables(request.getTemplateVariables())
                            .from(request.getFrom())
                            .replyTo(request.getReplyTo())
                            .attachments(request.getAttachments())
                            .build();
                    log.debug("Replaced static asset references in email body");
                }
            } catch (Exception e) {
                log.warn("Failed to replace static asset references, continuing with original body", e);
                // Continue with original body if replacement fails
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

    /**
     * Validate that template exists and is active.
     * Throws TemplateNotFoundException if not found.
     */
    private void validateTemplate(String templateName, NotificationType notificationType) {
        templateService.renderTemplate(notificationType, templateName, Map.of());
    }

    /**
     * Check if queue mode is enabled.
     *
     * @return true if queue mode is enabled, false otherwise
     */
    private boolean isQueueModeEnabled() {
        return properties.getQueue() != null 
                && properties.getQueue().isEnabled()
                && properties.getPersistence() != null
                && properties.getPersistence().isEnabled()
                && !isRabbitMQModeEnabled(); // Queue mode is disabled when RabbitMQ is enabled
    }
    
    /**
     * Check if RabbitMQ mode is enabled.
     *
     * @return true if RabbitMQ mode is enabled, false otherwise
     */
    private boolean isRabbitMQModeEnabled() {
        return properties.getAsync() != null
                && "rabbitmq".equalsIgnoreCase(properties.getAsync().getMode())
                && rabbitMQPublisher != null
                && rabbitTemplate != null;
    }
    
    /**
     * Check if persistence is enabled.
     *
     * @return true if persistence is enabled, false otherwise
     */
    private boolean isPersistenceEnabled() {
        return persistenceService != null
                && properties.getPersistence() != null
                && properties.getPersistence().isEnabled();
    }
    
    /**
     * Publish notification to RabbitMQ.
     * Persists to DB first, then publishes message with attachment paths.
     */
    private CompletableFuture<NotificationResponse> publishToRabbitMQ(NotificationRequest request) {
        log.debug("Publishing notification to RabbitMQ");
        
        // Persist to database first (required for RabbitMQ mode)
        if (!isPersistenceEnabled()) {
            log.error("Persistence must be enabled for RabbitMQ mode");
            return CompletableFuture.completedFuture(
                    NotificationResponse.failure(
                            "Persistence must be enabled for RabbitMQ mode",
                            "rabbitmq",
                            request.getNotificationType()
                    )
            );
        }
        
        try {
            // Replace static asset references (cid:) with URLs for email notifications before persisting
            if (request.getNotificationType() == NotificationType.EMAIL && request.getBody() != null && staticAssetService != null) {
                try {
                    String bodyWithUrls = staticAssetService.replaceStaticAssetReferences(request.getBody());
                    if (!bodyWithUrls.equals(request.getBody())) {
                        request = NotificationRequest.builder()
                                .notificationType(request.getNotificationType())
                                .to(request.getTo())
                                .cc(request.getCc())
                                .bcc(request.getBcc())
                                .subject(request.getSubject())
                                .body(bodyWithUrls) // Body now has URLs instead of cid:
                                .templateName(request.getTemplateName())
                                .templateVariables(request.getTemplateVariables())
                                .from(request.getFrom())
                                .replyTo(request.getReplyTo())
                                .attachments(request.getAttachments())
                                .build();
                        log.debug("Replaced static asset references in email body before persisting");
                    }
                } catch (Exception e) {
                    log.warn("Failed to replace static asset references, continuing with original body", e);
                    // Continue with original body if replacement fails
                }
            }
            
            // Persist notification to DB with PENDING status
            NotificationEntity entity = persistenceService.persistBeforeSend(request);
            log.info("Notification persisted with ID: {}, publishing to RabbitMQ", entity.getId());
            
            // Convert entity to message (includes attachment paths from DB)
            NotificationMessage message = rabbitMQPublisher.convertEntityToMessage(entity);
            
            // Determine routing key and exchange
            String routingKey = getRoutingKeyForType(request.getNotificationType());
            String exchange = properties.getRabbitmq().getExchange();
            
            // Publish to RabbitMQ
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            
            log.info("Notification published to RabbitMQ: exchange={}, routingKey={}, notificationId={}", 
                    exchange, routingKey, entity.getId());
            
            // Return success response immediately (consumer will update status)
            return CompletableFuture.completedFuture(
                    NotificationResponse.builder()
                            .success(true)
                            .messageId("RABBITMQ-QUEUED-" + entity.getId())
                            .provider("rabbitmq")
                            .notificationType(request.getNotificationType())
                            .message("Notification queued to RabbitMQ successfully")
                            .build()
            );
            
        } catch (Exception e) {
            log.error("Failed to publish notification to RabbitMQ", e);
            return CompletableFuture.completedFuture(
                    NotificationResponse.failure(
                            "Failed to publish to RabbitMQ: " + e.getMessage(),
                            "rabbitmq",
                            request.getNotificationType()
                    )
            );
        }
    }
    
    /**
     * Get routing key based on notification type.
     */
    private String getRoutingKeyForType(NotificationType notificationType) {
        NotificationProperties.RabbitMQConfig rabbitmqConfig = properties.getRabbitmq();
        switch (notificationType) {
            case EMAIL:
                return rabbitmqConfig.getRoutingKey().getEmail();
            case SMS:
                return rabbitmqConfig.getRoutingKey().getSms();
            case WHATSAPP:
                return rabbitmqConfig.getRoutingKey().getWhatsapp();
            default:
                throw new IllegalArgumentException("Unsupported notification type: " + notificationType);
        }
    }
}
