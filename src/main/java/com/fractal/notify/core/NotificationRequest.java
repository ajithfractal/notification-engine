package com.fractal.notify.core;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Unified request model for all notification types.
 */
@Data
@Builder
public class NotificationRequest {
    /**
     * Type of notification (EMAIL, SMS, WHATSAPP)
     */
    private NotificationType notificationType;

    /**
     * Recipient addresses (email addresses or phone numbers) - supports multiple recipients
     */
    private List<String> to;

    /**
     * CC recipients (for email notifications)
     */
    private List<String> cc;

    /**
     * BCC recipients (for email notifications)
     */
    private List<String> bcc;

    /**
     * Subject for email notifications
     */
    private String subject;

    /**
     * Body/content of the notification
     */
    private String body;

    /**
     * Template name to use (optional) - loads from resources
     */
    private String templateName;

    /**
     * Template content provided by client (optional) - takes precedence over templateName
     */
    private String templateContent;

    /**
     * Variables to be used in template rendering
     */
    private Map<String, Object> templateVariables;

    /**
     * From address (optional, uses default if not provided)
     */
    private String from;
}
