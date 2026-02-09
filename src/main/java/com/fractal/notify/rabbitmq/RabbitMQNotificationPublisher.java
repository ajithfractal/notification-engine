package com.fractal.notify.rabbitmq;

import com.fractal.notify.config.NotificationProperties;
import com.fractal.notify.core.NotificationRequest;
import com.fractal.notify.core.NotificationResponse;
import com.fractal.notify.core.NotificationType;
import com.fractal.notify.email.dto.EmailAttachment;
import com.fractal.notify.persistence.entity.EmailAttachmentEntity;
import com.fractal.notify.persistence.entity.NotificationEntity;
import com.fractal.notify.persistence.repository.EmailAttachmentRepository;
import com.fractal.notify.rabbitmq.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * RabbitMQ implementation of AsyncNotificationPublisher.
 * Publishes notification messages to RabbitMQ queues.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "fractal.notify.async",
    name = "mode",
    havingValue = "rabbitmq"
)
public class RabbitMQNotificationPublisher implements com.fractal.notify.async.AsyncNotificationPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final NotificationProperties properties;
    private final EmailAttachmentRepository emailAttachmentRepository;

    @Override
    public CompletableFuture<NotificationResponse> publishAsync(NotificationRequest request) {
        try {
            log.debug("Publishing notification to RabbitMQ: type={}, to={}", 
                    request.getNotificationType(), request.getTo());

            // Convert NotificationRequest to NotificationMessage
            NotificationMessage message = convertToMessage(request);

            // Determine routing key based on notification type
            String routingKey = getRoutingKey(request.getNotificationType());
            String exchange = properties.getRabbitmq().getExchange();

            // Publish to RabbitMQ
            rabbitTemplate.convertAndSend(exchange, routingKey, message);

            log.info("Notification published to RabbitMQ: exchange={}, routingKey={}, notificationId={}", 
                    exchange, routingKey, message.getNotificationId());

            // Return success response immediately (consumer will handle actual sending)
            return CompletableFuture.completedFuture(
                    NotificationResponse.builder()
                            .success(true)
                            .messageId("RABBITMQ-QUEUED-" + message.getNotificationId())
                            .provider("rabbitmq")
                            .notificationType(request.getNotificationType())
                            .message("Notification queued to RabbitMQ successfully")
                            .build()
            );

        } catch (Exception e) {
            log.error("Error publishing notification to RabbitMQ", e);
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
     * Convert NotificationRequest to NotificationMessage.
     * Note: This assumes the notification has already been persisted to DB
     * and attachments have been uploaded to storage.
     */
    private NotificationMessage convertToMessage(NotificationRequest request) {
        // Load attachment paths from database if this is an email notification
        List<NotificationMessage.AttachmentPath> attachmentPaths = null;
        if (request.getNotificationType() == NotificationType.EMAIL 
                && request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            attachmentPaths = new ArrayList<>();
            // Note: We need the notification ID to load attachments
            // This will be set by NotificationService before calling this
        }

        return NotificationMessage.builder()
                .notificationType(request.getNotificationType().name())
                .to(request.getTo())
                .cc(request.getCc())
                .bcc(request.getBcc())
                .subject(request.getSubject())
                .body(request.getBody())
                .templateName(request.getTemplateName())
                .templateVariables(request.getTemplateVariables())
                .from(request.getFrom())
                .replyTo(request.getReplyTo())
                .attachmentPaths(attachmentPaths)
                .retryCount(0)
                .build();
    }

    /**
     * Get routing key based on notification type.
     */
    private String getRoutingKey(NotificationType notificationType) {
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

    @Override
    public String getPublisherType() {
        return "rabbitmq";
    }

    /**
     * Convert NotificationEntity to NotificationMessage with attachment paths.
     * This is called by NotificationService after persisting to DB.
     */
    public NotificationMessage convertEntityToMessage(NotificationEntity entity) {
        List<NotificationMessage.AttachmentPath> attachmentPaths = null;
        
        // Load attachment paths if this is an email notification
        if (entity.getNotificationType() == NotificationType.EMAIL) {
            List<EmailAttachmentEntity> attachmentEntities = 
                    emailAttachmentRepository.findByNotificationId(entity.getId());
            
            if (attachmentEntities != null && !attachmentEntities.isEmpty()) {
                attachmentPaths = new ArrayList<>();
                for (EmailAttachmentEntity attachmentEntity : attachmentEntities) {
                    attachmentPaths.add(NotificationMessage.AttachmentPath.builder()
                            .storagePath(attachmentEntity.getStoragePath())
                            .fileName(attachmentEntity.getFileName())
                            .contentType(attachmentEntity.getContentType())
                            .isInline(attachmentEntity.getIsInline())
                            .contentId(attachmentEntity.getContentId())
                            .build());
                }
            }
        }

        return NotificationMessage.builder()
                .notificationId(entity.getId())
                .notificationType(entity.getNotificationType().name())
                .to(entity.getRecipientTo() != null ? List.of(entity.getRecipientTo()) : null)
                .cc(entity.getRecipientCc() != null ? List.of(entity.getRecipientCc()) : null)
                .bcc(entity.getRecipientBcc() != null ? List.of(entity.getRecipientBcc()) : null)
                .subject(entity.getSubject())
                .body(entity.getBody())
                .templateName(entity.getTemplateName())
                .templateVariables(entity.getTemplateVariables())
                .from(entity.getFromAddress())
                .attachmentPaths(attachmentPaths)
                .retryCount(entity.getRetryCount() != null ? entity.getRetryCount() : 0)
                .build();
    }
}
