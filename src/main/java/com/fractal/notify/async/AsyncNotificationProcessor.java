package com.fractal.notify.async;

import com.fractal.notify.core.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Component for processing notifications asynchronously.
 */
@Slf4j
@Component
public class AsyncNotificationProcessor {

    /**
     * Process a notification sending operation asynchronously.
     *
     * @param notificationSupplier the supplier that sends the notification
     * @return CompletableFuture with the notification response
     */
    @Async("notificationExecutor")
    public CompletableFuture<NotificationResponse> processAsync(Supplier<NotificationResponse> notificationSupplier) {
        try {
            log.debug("Processing notification asynchronously");
            NotificationResponse response = notificationSupplier.get();
            return CompletableFuture.completedFuture(response);
        } catch (Exception e) {
            log.error("Error in async notification processing", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
