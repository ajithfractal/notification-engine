package com.fractal.notify.persistence;

import com.fractal.notify.core.NotificationRequest;
import com.fractal.notify.core.NotificationResponse;
import com.fractal.notify.persistence.entity.NotificationEntity;
import com.fractal.notify.persistence.entity.NotificationStatus;
import com.fractal.notify.persistence.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Service for persisting notifications to the database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "fractal.notify.persistence", name = "enabled", havingValue = "true", matchIfMissing = false)
public class NotificationPersistenceService {
    private final NotificationRepository notificationRepository;

    /**
     * Persist a notification request before sending.
     *
     * @param request the notification request
     * @return the persisted entity with generated ID
     */
    @Transactional
    public NotificationEntity persistBeforeSend(NotificationRequest request) {
        NotificationEntity entity = NotificationEntity.builder()
                .notificationType(request.getNotificationType())
                .recipientTo(convertListToArray(request.getTo()))
                .recipientCc(convertListToArray(request.getCc()))
                .recipientBcc(convertListToArray(request.getBcc()))
                .subject(request.getSubject())
                .body(request.getBody())
                .templateName(request.getTemplateName())
                .templateContent(request.getTemplateContent())
                .templateVariables(request.getTemplateVariables())
                .fromAddress(request.getFrom())
                .status(NotificationStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        NotificationEntity saved = notificationRepository.save(entity);
        log.debug("Notification persisted with ID: {}", saved.getId());
        return saved;
    }

    /**
     * Update notification status after sending.
     *
     * @param entityId the entity ID
     * @param response the notification response
     * @param provider the provider name
     */
    @Transactional
    public void updateAfterSend(Long entityId, NotificationResponse response, String provider) {
        NotificationEntity entity = notificationRepository.findById(entityId)
                .orElseThrow(() -> new RuntimeException("Notification entity not found: " + entityId));

        entity.setStatus(response.isSuccess() ? NotificationStatus.SENT : NotificationStatus.FAILED);
        entity.setProvider(provider);
        entity.setMessageId(response.getMessageId());
        entity.setErrorMessage(response.getErrorMessage());
        
        if (response.isSuccess()) {
            entity.setSentAt(OffsetDateTime.now());
        }
        
        entity.setUpdatedAt(OffsetDateTime.now());

        notificationRepository.save(entity);
        log.debug("Notification {} updated with status: {}", entityId, entity.getStatus());
    }

    /**
     * Get all pending notifications (for retry mechanism).
     *
     * @return list of pending notifications
     */
    public List<NotificationEntity> getPendingNotifications() {
        return notificationRepository.findByStatus(NotificationStatus.PENDING);
    }

    /**
     * Convert List<String> to String[] for PostgreSQL array storage.
     */
    private String[] convertListToArray(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.toArray(new String[0]);
    }
}
