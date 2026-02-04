package com.fractal.notify.persistence.repository;

import com.fractal.notify.persistence.entity.NotificationEntity;
import com.fractal.notify.persistence.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

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
}
