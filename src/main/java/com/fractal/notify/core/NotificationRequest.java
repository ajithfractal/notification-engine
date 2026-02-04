package com.fractal.notify.core;

import lombok.Builder;
import lombok.Data;

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
     * Recipient address (email address or phone number)
     */
    private String to;

    /**
     * Subject for email notifications
     */
    private String subject;

    /**
     * Body/content of the notification
     */
    private String body;

    /**
     * Template name to use (optional)
     */
    private String templateName;

    /**
     * Variables to be used in template rendering
     */
    private Map<String, Object> templateVariables;

    /**
     * From address (optional, uses default if not provided)
     */
    private String from;
}
