package com.fractal.notify.async;

import com.fractal.notify.core.NotificationRequest;
import com.fractal.notify.core.NotificationResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Abstraction layer for async notification publishing.

 * Current implementation uses @Async, but you can add other implementations
 * like RabbitMQ, Redis, or other message brokers by implementing this interface.
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

     * @return the publisher type (e.g., "async", "rabbitmq")
     */
    String getPublisherType();
}
