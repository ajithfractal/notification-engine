package com.fractal.notify.persistence.entity;

/**
 * Status of a notification in the system.
 */
public enum NotificationStatus {
    PENDING,
    SENT,
    FAILED,
    RETRYING
}
