package com.fractal.notify.persistence.repository;

import com.fractal.notify.persistence.entity.NotificationEntity;
import com.fractal.notify.persistence.entity.NotificationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for notification entities.
 */
@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    /**
     * Find all notifications with a specific status.
     *
     * @param status the status to search for
     * @return list of notifications with the given status
     */
    List<NotificationEntity> findByStatus(NotificationStatus status);

    /**
     * Find all pending notifications created before a specific time.
     * Useful for retry mechanisms.
     *
     * @param status the status (typically PENDING)
     * @param beforeTime the time threshold
     * @return list of notifications
     */
    List<NotificationEntity> findByStatusAndCreatedAtBefore(NotificationStatus status, OffsetDateTime beforeTime);

    /**
     * Find pending notifications for queue processing with pessimistic locking.
     * This prevents multiple schedulers from processing the same notification.
     * Orders by created_at ASC to process oldest first.
     *
     * @param status the status (PENDING)
     * @param limit maximum number of notifications to fetch
     * @return list of locked notifications ready for processing
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM NotificationEntity n WHERE n.status = :status ORDER BY n.createdAt ASC")
    List<NotificationEntity> findPendingNotificationsForProcessing(@Param("status") NotificationStatus status, org.springframework.data.domain.Pageable pageable);

    /**
     * Find a notification by ID with pessimistic lock.
     * Used to prevent duplicate processing.
     *
     * @param id the notification ID
     * @return optional notification entity
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM NotificationEntity n WHERE n.id = :id")
    Optional<NotificationEntity> findByIdWithLock(@Param("id") Long id);

    /**
     * Find retrying notifications that are ready for retry.
     * Only includes notifications where enough time has passed since last update (based on retry delay).
     *
     * @param status the status (RETRYING)
     * @param retryTime the time threshold for retry
     * @param pageable pagination information
     * @return list of retrying notifications ready for retry
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM NotificationEntity n WHERE n.status = :status AND n.updatedAt <= :retryTime ORDER BY n.updatedAt ASC")
    List<NotificationEntity> findRetryingNotificationsForProcessing(@Param("status") NotificationStatus status, @Param("retryTime") OffsetDateTime retryTime, org.springframework.data.domain.Pageable pageable);

    /**
     * Find stuck PROCESSING notifications (e.g., from crashed application).
     * These are notifications that have been in PROCESSING status for too long.
     *
     * @param status the status (PROCESSING)
     * @param stuckTime the time threshold (notifications older than this are considered stuck)
     * @param pageable pagination information
     * @return list of stuck notifications
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM NotificationEntity n WHERE n.status = :status AND n.updatedAt <= :stuckTime ORDER BY n.updatedAt ASC")
    List<NotificationEntity> findStuckProcessingNotifications(@Param("status") NotificationStatus status, @Param("stuckTime") OffsetDateTime stuckTime, org.springframework.data.domain.Pageable pageable);

    /**
     * Find sent notifications with matching criteria for duplicate detection.
     * Used to prevent sending duplicate emails.
     *
     * @param subject the subject
     * @param body the body content
     * @param sinceTime check for duplicates since this time
     * @return list of potentially duplicate notifications
     */
    @Query("SELECT n FROM NotificationEntity n WHERE " +
           "n.subject = :subject AND " +
           "n.body = :body AND " +
           "n.status = 'SENT' AND " +
           "n.sentAt >= :sinceTime")
    List<NotificationEntity> findPotentialDuplicates(
            @Param("subject") String subject,
            @Param("body") String body,
            @Param("sinceTime") OffsetDateTime sinceTime
    );
}
