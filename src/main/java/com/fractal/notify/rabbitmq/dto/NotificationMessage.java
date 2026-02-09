package com.fractal.notify.rabbitmq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Message DTO for RabbitMQ notifications.
 * This is the message structure sent to RabbitMQ queues.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    /**
     * Notification ID from database (for status updates by consumer)
     */
    @JsonProperty("notificationId")
    private Long notificationId;

    /**
     * Type of notification (EMAIL, SMS, WHATSAPP)
     */
    @JsonProperty("notificationType")
    private String notificationType;

    /**
     * Recipient addresses
     */
    @JsonProperty("to")
    private List<String> to;

    /**
     * CC recipients (for email)
     */
    @JsonProperty("cc")
    private List<String> cc;

    /**
     * BCC recipients (for email)
     */
    @JsonProperty("bcc")
    private List<String> bcc;

    /**
     * Subject (for email)
     */
    @JsonProperty("subject")
    private String subject;

    /**
     * Body/content
     */
    @JsonProperty("body")
    private String body;

    /**
     * Template name (optional)
     */
    @JsonProperty("templateName")
    private String templateName;

    /**
     * Template variables
     */
    @JsonProperty("templateVariables")
    private Map<String, Object> templateVariables;

    /**
     * From address
     */
    @JsonProperty("from")
    private String from;

    /**
     * Reply-To address (for email)
     */
    @JsonProperty("replyTo")
    private String replyTo;

    /**
     * Attachment storage paths (not the actual files)
     */
    @JsonProperty("attachmentPaths")
    private List<AttachmentPath> attachmentPaths;

    /**
     * Retry count
     */
    @JsonProperty("retryCount")
    private Integer retryCount;

    /**
     * Attachment path information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentPath {
        @JsonProperty("storagePath")
        private String storagePath;

        @JsonProperty("fileName")
        private String fileName;

        @JsonProperty("contentType")
        private String contentType;

        @JsonProperty("isInline")
        private Boolean isInline;

        @JsonProperty("contentId")
        private String contentId;
    }
}
