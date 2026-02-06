package com.fractal.notify.persistence.repository;

import com.fractal.notify.core.NotificationType;
import com.fractal.notify.persistence.entity.TemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TemplateRepository extends JpaRepository<TemplateEntity, Long> {
    Optional<TemplateEntity> findByNameAndNotificationTypeAndIsActiveTrue(
        String name, 
        NotificationType notificationType
    );
}
