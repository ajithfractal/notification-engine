package com.fractal.notify.queue;

import com.fractal.notify.config.NotificationProperties;
import com.fractal.notify.core.NotificationRequest;
import com.fractal.notify.core.NotificationResponse;
import com.fractal.notify.core.NotificationService;
import com.fractal.notify.core.NotificationType;
import com.fractal.notify.email.dto.EmailAttachment;
import com.fractal.notify.persistence.entity.EmailAttachmentEntity;
import com.fractal.notify.persistence.entity.NotificationEntity;
import com.fractal.notify.persistence.entity.NotificationStatus;
import com.fractal.notify.persistence.repository.EmailAttachmentRepository;
import com.fractal.notify.persistence.repository.NotificationRepository;
import com.fractal.notify.storage.StorageException;
import com.fractal.notify.storage.StorageProvider;
import com.fractal.notify.storage.StorageProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Queue processor that polls the database for pending notifications and sends them.
 * Uses pessimistic locking to prevent duplicate processing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "fractal.notify.queue", name = "enabled", havingValue = "true", matchIfMissing = false)
public class NotificationQueueProcessor {
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final NotificationProperties properties;
    private final EmailAttachmentRepository emailAttachmentRepository;
    private final StorageProviderFactory storageProviderFactory;

    /**
     * Process pending notifications from the queue.
     * Runs on a configurable schedule.
     * Uses pessimistic locking to prevent duplicate processing.
     */
    @Scheduled(fixedDelayString = "${fractal.notify.queue.poll-interval:5000}")
    @Transactional
    public void processQueue() {
        // Check if queue processing is enabled
        if (!isQueueEnabled()) {
            return;
        }

        try {
            int batchSize = properties.getQueue().getBatchSize();
            PageRequest pageRequest = PageRequest.of(0, batchSize);
            
            // Fetch pending notifications with pessimistic lock
            List<NotificationEntity> pendingNotifications = new ArrayList<>(
                    notificationRepository.findPendingNotificationsForProcessing(NotificationStatus.PENDING, pageRequest)
            );
            
            // Also fetch retrying notifications that are ready for retry
            long retryDelayMs = properties.getQueue().getRetryDelay();
            OffsetDateTime retryTime = OffsetDateTime.now().minusSeconds(retryDelayMs / 1000);
            List<NotificationEntity> retryingNotifications = notificationRepository
                    .findRetryingNotificationsForProcessing(NotificationStatus.RETRYING, retryTime, pageRequest);
            
            // Also fetch stuck PROCESSING notifications (from crashed application)
            // Consider notifications stuck if they've been PROCESSING for more than 5 minutes
            OffsetDateTime stuckTime = OffsetDateTime.now().minusMinutes(5);
            List<NotificationEntity> stuckNotifications = notificationRepository
                    .findStuckProcessingNotifications(NotificationStatus.PROCESSING, stuckTime, pageRequest);
            
            // Combine all lists
            if (retryingNotifications != null && !retryingNotifications.isEmpty()) {
                pendingNotifications.addAll(retryingNotifications);
            }
            if (stuckNotifications != null && !stuckNotifications.isEmpty()) {
                // Reset stuck notifications back to PENDING
                for (NotificationEntity stuck : stuckNotifications) {
                    stuck.setStatus(NotificationStatus.PENDING);
                    stuck.setUpdatedAt(OffsetDateTime.now());
                    notificationRepository.save(stuck);
                    pendingNotifications.add(stuck);
                }
                log.warn("Found {} stuck PROCESSING notifications, resetting to PENDING", stuckNotifications.size());
            }

            if (pendingNotifications.isEmpty()) {
                log.debug("No pending notifications to process");
                return;
            }

            log.info("Processing {} pending notifications from queue", pendingNotifications.size());

            for (NotificationEntity entity : pendingNotifications) {
                try {
                    // Mark as PROCESSING to prevent duplicate processing
                    entity.setStatus(NotificationStatus.PROCESSING);
                    entity.setUpdatedAt(OffsetDateTime.now());
                    notificationRepository.save(entity);

                    // Convert entity to NotificationRequest
                    NotificationRequest request = convertEntityToRequest(entity);

                    // Validate request before sending
                    validateRequest(request);

                    // Check for duplicate notification (prevent sending same email twice)
                    if (isDuplicateNotification(entity)) {
                        log.warn("Duplicate notification detected for ID: {}. Skipping to prevent duplicate email.", entity.getId());
                        entity.setStatus(NotificationStatus.SENT);
                        entity.setMessageId("DUPLICATE-SKIPPED");
                        entity.setErrorMessage("Duplicate notification - skipped to prevent duplicate email");
                        entity.setUpdatedAt(OffsetDateTime.now());
                        notificationRepository.save(entity);
                        continue;
                    }

                    // Send notification synchronously (we're already in async context via scheduler)
                    NotificationResponse response = notificationService.send(request);

                    // Update entity with result
                    updateEntityAfterSend(entity, response);

                    log.info("Notification {} processed successfully. Status: {}, MessageId: {}", 
                            entity.getId(), 
                            response.isSuccess() ? "SENT" : "FAILED",
                            response.getMessageId());

                } catch (Exception e) {
                    log.error("Error processing notification ID: {}", entity.getId(), e);
                    handleProcessingError(entity, e);
                }
            }

        } catch (Exception e) {
            log.error("Error in queue processor", e);
        }
    }

    /**
     * Check if queue processing is enabled.
     */
    private boolean isQueueEnabled() {
        return properties.getQueue() != null 
                && properties.getQueue().isEnabled()
                && properties.getPersistence() != null
                && properties.getPersistence().isEnabled();
    }

    /**
     * Convert NotificationEntity to NotificationRequest.
     */
    private NotificationRequest convertEntityToRequest(NotificationEntity entity) {
        NotificationRequest.NotificationRequestBuilder builder = NotificationRequest.builder()
                .notificationType(entity.getNotificationType())
                .to(entity.getRecipientTo() != null ? Arrays.asList(entity.getRecipientTo()) : null)
                .cc(entity.getRecipientCc() != null ? Arrays.asList(entity.getRecipientCc()) : null)
                .bcc(entity.getRecipientBcc() != null ? Arrays.asList(entity.getRecipientBcc()) : null)
                .subject(entity.getSubject())
                .body(entity.getBody())
                .templateName(entity.getTemplateName())
                .templateVariables(entity.getTemplateVariables())
                .from(entity.getFromAddress());

        // Load and download attachments if this is an EMAIL notification
        if (entity.getNotificationType() == NotificationType.EMAIL) {
            List<EmailAttachment> attachments = loadAttachments(entity.getId());
            if (attachments != null && !attachments.isEmpty()) {
                builder.attachments(attachments);
                log.info("Loaded {} attachment(s) for notification {}", attachments.size(), entity.getId());
            } else {
                log.debug("No attachments found for notification {}", entity.getId());
            }
        }

        return builder.build();
    }

    /**
     * Load attachments from database and download from storage.
     *
     * @param notificationId the notification ID
     * @return list of EmailAttachment DTOs
     */
    private List<EmailAttachment> loadAttachments(Long notificationId) {
        List<EmailAttachmentEntity> attachmentEntities = emailAttachmentRepository.findByNotificationId(notificationId);
        
        if (attachmentEntities == null || attachmentEntities.isEmpty()) {
            log.debug("No attachment entities found in database for notification {}", notificationId);
            return null;
        }

        log.info("Found {} attachment(s) in database for notification {}", attachmentEntities.size(), notificationId);
        StorageProvider storageProvider = storageProviderFactory.getProvider();
        List<EmailAttachment> attachments = new ArrayList<>();

        for (EmailAttachmentEntity entity : attachmentEntities) {
            try {
                log.debug("Downloading attachment: {} from storage path: {}", entity.getFileName(), entity.getStoragePath());
                
                // Download from storage
                InputStream inputStream = storageProvider.download(entity.getStoragePath());
                
                // Read into byte array for EmailAttachment
                byte[] bytes = inputStream.readAllBytes();
                inputStream.close();

                // Fix content type if it's "multipart/form-data" (HTTP form type, not file type)
                String contentType = entity.getContentType();
                if (contentType != null && contentType.equals("multipart/form-data")) {
                    // Try to detect from file extension
                    String fileName = entity.getFileName().toLowerCase();
                    if (fileName.endsWith(".pdf")) {
                        contentType = "application/pdf";
                    } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                        contentType = "image/jpeg";
                    } else if (fileName.endsWith(".png")) {
                        contentType = "image/png";
                    } else if (fileName.endsWith(".gif")) {
                        contentType = "image/gif";
                    } else {
                        contentType = "application/octet-stream";
                    }
                    log.warn("Fixed content type from 'multipart/form-data' to '{}' for file: {}", contentType, entity.getFileName());
                }

                // Convert to EmailAttachment DTO
                EmailAttachment attachment = EmailAttachment.builder()
                        .bytes(bytes)
                        .fileName(entity.getFileName())
                        .contentType(contentType)
                        .isInline(entity.getIsInline() != null && entity.getIsInline())
                        .contentId(entity.getContentId())
                        .build();

                attachments.add(attachment);
                log.info("Successfully loaded attachment: {} ({} bytes, type: {}) for notification {}", 
                        entity.getFileName(), bytes.length, contentType, notificationId);

            } catch (StorageException e) {
                log.error("Failed to download attachment {} for notification {} from path: {}", 
                        entity.getFileName(), notificationId, entity.getStoragePath(), e);
                // Continue with other attachments even if one fails
            } catch (IOException e) {
                log.error("Failed to read attachment {} for notification {}", 
                        entity.getFileName(), notificationId, e);
                // Continue with other attachments even if one fails
            }
        }

        if (attachments.isEmpty()) {
            log.warn("No attachments were successfully loaded for notification {} (all failed)", notificationId);
            return null;
        }

        return attachments;
    }

    /**
     * Validate notification request before sending.
     */
    private void validateRequest(NotificationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Notification request cannot be null");
        }
        
        if (request.getNotificationType() == null) {
            throw new IllegalArgumentException("Notification type is required");
        }
        
        if (request.getTo() == null || request.getTo().isEmpty()) {
            throw new IllegalArgumentException("At least one recipient is required");
        }
        
        // Type-specific validations
        if (request.getNotificationType() == com.fractal.notify.core.NotificationType.EMAIL) {
            if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
                throw new IllegalArgumentException("Subject is required for email notifications");
            }
        }
        
        // Validate that either body or template is provided
        boolean hasBody = request.getBody() != null && !request.getBody().trim().isEmpty();
        boolean hasTemplate = request.getTemplateName() != null && !request.getTemplateName().trim().isEmpty();
        
        if (!hasBody && !hasTemplate) {
            throw new IllegalArgumentException(
                "Either body or templateName must be provided"
            );
        }
    }

    /**
     * Update entity after sending notification.
     */
    private void updateEntityAfterSend(NotificationEntity entity, NotificationResponse response) {
        entity.setStatus(response.isSuccess() ? NotificationStatus.SENT : NotificationStatus.FAILED);
        entity.setProvider(response.getProvider());
        entity.setMessageId(response.getMessageId());
        entity.setErrorMessage(response.getErrorMessage());
        
        if (response.isSuccess()) {
            entity.setSentAt(OffsetDateTime.now());
        } else {
            // Increment retry count
            entity.setRetryCount((entity.getRetryCount() != null ? entity.getRetryCount() : 0) + 1);
            
            // Check if we should retry
            int maxRetries = properties.getQueue().getMaxRetries();
            if (entity.getRetryCount() < maxRetries) {
                entity.setStatus(NotificationStatus.RETRYING);
                log.info("Notification {} will be retried. Retry count: {}/{}", 
                        entity.getId(), entity.getRetryCount(), maxRetries);
            } else {
                log.warn("Notification {} exceeded max retries ({}). Marking as FAILED.", 
                        entity.getId(), maxRetries);
            }
        }
        
        entity.setUpdatedAt(OffsetDateTime.now());
        notificationRepository.save(entity);
    }

    /**
     * Check if this notification is a duplicate of a recently sent notification.
     * Prevents sending the same email multiple times.
     * Compares recipients, subject, and body content.
     *
     * @param entity the notification entity to check
     * @return true if duplicate, false otherwise
     */
    private boolean isDuplicateNotification(NotificationEntity entity) {
        // Only check for duplicates if it's an email notification
        if (entity.getNotificationType() != com.fractal.notify.core.NotificationType.EMAIL) {
            return false;
        }

        // Check for duplicates within the last 1 hour
        OffsetDateTime oneHourAgo = OffsetDateTime.now().minusHours(1);
        
        try {
            // Find potential duplicates (same subject and body)
            List<NotificationEntity> potentialDuplicates = notificationRepository.findPotentialDuplicates(
                    entity.getSubject(),
                    entity.getBody(),
                    oneHourAgo
            );

            if (potentialDuplicates.isEmpty()) {
                return false;
            }

            // Compare recipients array to find exact match
            String[] currentRecipients = entity.getRecipientTo();
            if (currentRecipients == null || currentRecipients.length == 0) {
                return false;
            }

            // Sort arrays for comparison
            Arrays.sort(currentRecipients);

            for (NotificationEntity duplicate : potentialDuplicates) {
                if (duplicate.getId().equals(entity.getId())) {
                    continue; // Skip self
                }

                String[] duplicateRecipients = duplicate.getRecipientTo();
                if (duplicateRecipients == null || duplicateRecipients.length != currentRecipients.length) {
                    continue;
                }

                // Sort and compare
                String[] sortedDuplicateRecipients = duplicateRecipients.clone();
                Arrays.sort(sortedDuplicateRecipients);

                if (Arrays.equals(currentRecipients, sortedDuplicateRecipients)) {
                    log.debug("Duplicate notification found: current ID={}, duplicate ID={}", 
                            entity.getId(), duplicate.getId());
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            log.warn("Error checking for duplicate notification: {}", e.getMessage());
            // If check fails, allow sending (fail open)
            return false;
        }
    }

    /**
     * Handle errors during notification processing.
     */
    private void handleProcessingError(NotificationEntity entity, Exception e) {
        entity.setStatus(NotificationStatus.FAILED);
        entity.setErrorMessage(e.getMessage());
        entity.setRetryCount((entity.getRetryCount() != null ? entity.getRetryCount() : 0) + 1);
        
        // Check if we should retry
        int maxRetries = properties.getQueue().getMaxRetries();
        if (entity.getRetryCount() < maxRetries) {
            entity.setStatus(NotificationStatus.RETRYING);
            log.info("Notification {} will be retried after error. Retry count: {}/{}", 
                    entity.getId(), entity.getRetryCount(), maxRetries);
        } else {
            log.error("Notification {} exceeded max retries after error. Marking as FAILED.", 
                    entity.getId());
        }
        
        entity.setUpdatedAt(OffsetDateTime.now());
        notificationRepository.save(entity);
    }
}
