package com.fractal.notify.core;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * Unified request model for all notification types.
 */
@Getter
@Setter
@Builder
public class NotificationRequest {
    /**
     * Type of notification (EMAIL, SMS, WHATSAPP)
     */
    private NotificationType notificationType;

    private List<String> to;

    private List<String> cc;

    private List<String> bcc;

    private String subject;

    /**
     * Body/content of the notification
     */
    private String body;

    /**
     * Template name to use (optional) - loads from database
     */
    private String templateName;

    /**
     * Variables to be used in template rendering
     */
    private Map<String, Object> templateVariables;

    private String from;

    /**
     * Reply-To address (for email notifications, optional, uses default from properties if not provided)
     */
    private String replyTo;

    /**
     * Attachments for email notifications (optional)
     */
    private List<com.fractal.notify.email.dto.EmailAttachment> attachments;
}
