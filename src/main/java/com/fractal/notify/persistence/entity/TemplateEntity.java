package com.fractal.notify.persistence.entity;

import com.fractal.notify.core.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "templates", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"name", "notification_type"}),
       indexes = {
           @Index(name = "idx_template_name_type", columnList = "name,notification_type"),
           @Index(name = "idx_template_active", columnList = "is_active")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 20)
    private NotificationType notificationType;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
