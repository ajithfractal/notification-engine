package com.fractal.notify.persistence.repository;

import com.fractal.notify.persistence.entity.EmailAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for email attachments.
 */
@Repository
public interface EmailAttachmentRepository extends JpaRepository<EmailAttachmentEntity, Long> {
    /**
     * Find all attachments for a notification.
     *
     * @param notificationId the notification ID
     * @return list of attachments
     */
    List<EmailAttachmentEntity> findByNotificationId(Long notificationId);
}
