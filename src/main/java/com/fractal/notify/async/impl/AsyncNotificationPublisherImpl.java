package com.fractal.notify.async.impl;

import com.fractal.notify.async.AsyncNotificationPublisher;
import com.fractal.notify.core.NotificationRequest;
import com.fractal.notify.core.NotificationResponse;
import com.fractal.notify.core.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Current implementation using Spring @Async for async processing.
 * When throughput increases, this can be replaced with KafkaPublisher implementation
 * by implementing the same AsyncNotificationPublisher interface.
 * This is the default implementation when mode is "async" or not specified.
 */
@Slf4j
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    prefix = "fractal.notify.async", 
    name = "mode", 
    havingValue = "async", 
    matchIfMissing = true
)
public class AsyncNotificationPublisherImpl implements AsyncNotificationPublisher {
    private final NotificationService notificationService;

    public AsyncNotificationPublisherImpl(@Lazy NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    @Async("notificationExecutor")
    public CompletableFuture<NotificationResponse> publishAsync(NotificationRequest request) {
        try {
            log.debug("Processing notification asynchronously via @Async");
            NotificationResponse response = notificationService.send(request);
            return CompletableFuture.completedFuture(response);
        } catch (Exception e) {
            log.error("Error in async notification processing", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public String getPublisherType() {
        return "async";
    }
}
