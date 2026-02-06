package com.fractal.notify.persistence.entity;

/**
 * Status of a notification in the system.
 */
public enum NotificationStatus {
    PENDING,      // Queued, waiting to be processed
    PROCESSING,   // Currently being processed by scheduler
    SENT,         // Successfully sent
    FAILED,       // Failed to send
    RETRYING      // Retrying after failure
}
