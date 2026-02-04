package com.fractal.notify.async;

import com.fractal.notify.core.NotificationRequest;
import com.fractal.notify.core.NotificationResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Abstraction layer for async notification publishing.
 * Current implementation uses @Async, future implementations can use Kafka.
 * This allows easy migration to Kafka when throughput increases.
 */
public interface AsyncNotificationPublisher {
    /**
     * Publish a notification request asynchronously.
     *
     * @param request the notification request
     * @return CompletableFuture with the notification response
     */
    CompletableFuture<NotificationResponse> publishAsync(NotificationRequest request);
    
    /**
     * Get the publisher type.
     *
     * @return the publisher type (e.g., "async", "kafka")
     */
    String getPublisherType();
}
