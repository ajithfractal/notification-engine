package com.fractal.notify.persistence;

import com.fractal.notify.core.NotificationRequest;
import com.fractal.notify.core.NotificationResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private final EmailAttachmentRepository emailAttachmentRepository;
    private final StorageProviderFactory storageProviderFactory;

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
                .templateVariables(request.getTemplateVariables())
                .fromAddress(request.getFrom())
                .status(NotificationStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        NotificationEntity saved = notificationRepository.save(entity);
        log.debug("Notification persisted with ID: {}", saved.getId());

        // Handle attachments if present (only for EMAIL notifications)
        if (request.getNotificationType() == com.fractal.notify.core.NotificationType.EMAIL
                && request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            uploadAndSaveAttachments(saved.getId(), request.getAttachments());
        }

        return saved;
    }

    /**
     * Upload attachments to storage and save metadata to database.
     *
     * @param notificationId the notification ID
     * @param attachments the list of attachments
     */
    private void uploadAndSaveAttachments(Long notificationId, List<EmailAttachment> attachments) {
        StorageProvider storageProvider = storageProviderFactory.getProvider();
        String providerName = storageProvider.getProviderName();

        for (EmailAttachment attachment : attachments) {
            try {
                String fileName = attachment.getFileName();
                String contentType = attachment.getContentType();
                
                // Check if this is a storage path reference (file already exists in storage)
                String storagePath;
                long fileSize;
                
                if (attachment.isStoragePathReference()) {
                    // Use existing storage path, skip upload
                    storagePath = attachment.getStoragePath();
                    fileSize = -1; // Unknown for storage references
                    log.debug("Using existing storage path for attachment: {} -> {}", fileName, storagePath);
                } else {
                    // Regular attachment - upload to storage
                    // Fix content type if it's "multipart/form-data" (HTTP form type, not file type)
                    if (contentType != null && contentType.equals("multipart/form-data")) {
                        // Try to detect from file extension
                        String fileNameLower = fileName.toLowerCase();
                        if (fileNameLower.endsWith(".pdf")) {
                            contentType = "application/pdf";
                        } else if (fileNameLower.endsWith(".jpg") || fileNameLower.endsWith(".jpeg")) {
                            contentType = "image/jpeg";
                        } else if (fileNameLower.endsWith(".png")) {
                            contentType = "image/png";
                        } else if (fileNameLower.endsWith(".gif")) {
                            contentType = "image/gif";
                        } else {
                            contentType = "application/octet-stream";
                        }
                        log.warn("Fixed content type from 'multipart/form-data' to '{}' for file: {}", contentType, fileName);
                    }
                    
                    InputStream inputStream = getInputStreamFromAttachment(attachment);
                    fileSize = getFileSizeFromAttachment(attachment);

                    // Upload to storage
                    log.debug("Uploading attachment: {} ({} bytes, type: {}) to storage", fileName, fileSize, contentType);
                    storagePath = storageProvider.upload(inputStream, fileName, contentType);
                    log.info("Successfully uploaded attachment: {} to storage path: {}", fileName, storagePath);
                }

                // Save metadata to database
                EmailAttachmentEntity attachmentEntity = EmailAttachmentEntity.builder()
                        .notificationId(notificationId)
                        .fileName(fileName)
                        .contentType(contentType)
                        .fileSize(fileSize > 0 ? fileSize : null)
                        .storageProvider(providerName)
                        .storagePath(storagePath)
                        .isInline(attachment.isInline())
                        .contentId(attachment.getContentId())
                        .createdAt(OffsetDateTime.now())
                        .build();

                emailAttachmentRepository.save(attachmentEntity);
                log.debug("Attachment uploaded and saved: {} for notification {}", fileName, notificationId);

            } catch (StorageException e) {
                log.error("Failed to upload attachment {} for notification {}", 
                        attachment.getFileName(), notificationId, e);
                // Continue with other attachments even if one fails
            } catch (IOException e) {
                log.error("Failed to read attachment {} for notification {}", 
                        attachment.getFileName(), notificationId, e);
                // Continue with other attachments even if one fails
            }
        }
    }

    /**
     * Get InputStream from attachment (handles File, byte[], and InputStream).
     */
    private InputStream getInputStreamFromAttachment(EmailAttachment attachment) throws IOException {
        if (attachment.getFile() != null) {
            return new FileInputStream(attachment.getFile());
        } else if (attachment.getBytes() != null) {
            return new java.io.ByteArrayInputStream(attachment.getBytes());
        } else if (attachment.getInputStream() != null) {
            return attachment.getInputStream();
        } else {
            throw new IllegalArgumentException("Attachment must have file, bytes, or inputStream");
        }
    }

    /**
     * Get file size from attachment.
     */
    private long getFileSizeFromAttachment(EmailAttachment attachment) {
        if (attachment.getFile() != null) {
            return attachment.getFile().length();
        } else if (attachment.getBytes() != null) {
            return attachment.getBytes().length;
        } else {
            // For InputStream, we can't determine size without reading it
            // Return -1 to indicate unknown size
            return -1;
        }
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
