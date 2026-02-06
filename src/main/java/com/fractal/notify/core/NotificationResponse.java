package com.fractal.notify.core;

import lombok.Builder;
import lombok.Data;

/**
 * Response model for notification operations.
 */
@Data
@Builder
public class NotificationResponse {
    /**
     * Whether the notification was sent successfully
     */
    private boolean success;

    /**
     * Error message if notification failed
     */
    private String errorMessage;

    /**
     * Provider-specific message ID or reference
     */
    private String messageId;

    /**
     * Provider name that was used
     */
    private String provider;

    /**
     * Notification type that was sent
     */
    private NotificationType notificationType;

    /**
     * Optional message (e.g., "Notification queued successfully")
     */
    private String message;

    /**
     * Creates a successful response
     */
    public static NotificationResponse success(String messageId, String provider, NotificationType type) {
        return NotificationResponse.builder()
                .success(true)
                .messageId(messageId)
                .provider(provider)
                .notificationType(type)
                .build();
    }

    /**
     * Creates a failed response
     */
    public static NotificationResponse failure(String errorMessage, String provider, NotificationType type) {
        return NotificationResponse.builder()
                .success(false)
                .errorMessage(errorMessage)
                .provider(provider)
                .notificationType(type)
                .build();
    }
}
