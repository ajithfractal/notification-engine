package com.fractal.notify.core;

/**
 * Strategy interface for different notification types.
 * Each notification type (Email, SMS, WhatsApp) implements this interface.
 */
public interface NotificationStrategy {
    /**
     * Send a notification based on the request.
     *
     * @param request the notification request
     * @return the notification response
     */
    NotificationResponse send(NotificationRequest request);

    /**
     * Get the notification type this strategy handles.
     *
     * @return the notification type
     */
    NotificationType getType();
}
