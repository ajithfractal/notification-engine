package com.fractal.notify.async.impl;

import com.fractal.notify.async.AsyncNotificationPublisher;
import com.fractal.notify.config.NotificationProperties;
import com.fractal.notify.core.NotificationRequest;
import com.fractal.notify.core.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka-based implementation of AsyncNotificationPublisher.
 * This will be used when throughput is high and Kafka is needed.
 * Currently disabled - can be enabled by setting fractal.notify.async.mode=kafka
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(prefix = "fractal.notify.async", name = "mode", havingValue = "kafka")
public class KafkaNotificationPublisher implements AsyncNotificationPublisher {
    private final KafkaTemplate<String, NotificationRequest> kafkaTemplate;
    private final NotificationProperties properties;

    @Override
    public CompletableFuture<NotificationResponse> publishAsync(NotificationRequest request) {
        String topic = properties.getAsync().getKafka().getTopic();
        
        log.debug("Publishing notification request to Kafka topic: {}", topic);
        
        CompletableFuture<SendResult<String, NotificationRequest>> future = kafkaTemplate.send(topic, request);
        
        return future.thenApply(result -> {
            log.debug("Notification request published successfully to topic: {}, partition: {}, offset: {}",
                    topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            return NotificationResponse.builder()
                    .success(true)
                    .messageId("kafka-" + result.getRecordMetadata().offset())
                    .provider("kafka")
                    .notificationType(request.getNotificationType())
                    .build();
        }).exceptionally(exception -> {
            log.error("Failed to publish notification request to Kafka topic: {}", topic, exception);
            return NotificationResponse.failure(
                    "Failed to publish to Kafka: " + exception.getMessage(),
                    "kafka",
                    request.getNotificationType()
            );
        });
    }

    @Override
    public String getPublisherType() {
        return "kafka";
    }
}
