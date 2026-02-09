package com.fractal.notify.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Entity representing an email attachment in the database.
 */
@Entity
@Table(name = "email_attachments", indexes = {
    @Index(name = "idx_email_attachment_notification_id", columnList = "notification_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailAttachmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", insertable = false, updatable = false)
    private NotificationEntity notification;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "storage_provider", nullable = false, length = 50)
    private String storageProvider;

    @Column(name = "storage_path", nullable = false, columnDefinition = "TEXT")
    private String storagePath;

    @Column(name = "is_inline")
    @Builder.Default
    private Boolean isInline = false;

    @Column(name = "content_id", length = 255)
    private String contentId;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
