# Adding a New Async Publisher Guide

This guide explains how to add a new async publisher implementation (e.g., RabbitMQ, Redis, AWS SQS) to the Fractal Notify system.

## Overview

The system uses an **AsyncNotificationPublisher** interface to abstract async notification publishing. The default implementation uses Spring `@Async`, but you can easily add other implementations like RabbitMQ, Redis, or any message broker.

---

## Step-by-Step Guide

### Example: Adding a RabbitMQ Async Publisher

#### Step 1: Add Dependencies

Add RabbitMQ dependencies to your `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

#### Step 2: Create the Publisher Implementation

Create a new class that implements `AsyncNotificationPublisher`:

```java
package com.fractal.notify.async.impl;

import com.fractal.notify.async.AsyncNotificationPublisher;
import com.fractal.notify.config.NotificationProperties;
import com.fractal.notify.core.NotificationRequest;
import com.fractal.notify.core.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * RabbitMQ-based implementation of AsyncNotificationPublisher.
 * This will be used when RabbitMQ is configured as the async publisher.
 * Enable by setting fractal.notify.async.mode=rabbitmq
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnClass(RabbitTemplate.class)
@ConditionalOnProperty(prefix = "fractal.notify.async", name = "mode", havingValue = "rabbitmq")
public class RabbitMQNotificationPublisher implements AsyncNotificationPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final NotificationProperties properties;

    @Override
    public CompletableFuture<NotificationResponse> publishAsync(NotificationRequest request) {
        String exchange = properties.getAsync().getRabbitmq().getExchange();
        String routingKey = properties.getAsync().getRabbitmq().getRoutingKey();
        
        log.debug("Publishing notification request to RabbitMQ exchange: {}, routingKey: {}", 
                exchange, routingKey);
        
        try {
            // Send message to RabbitMQ
            rabbitTemplate.convertAndSend(exchange, routingKey, request);
            
            log.debug("Notification request published successfully to RabbitMQ");
            
            // For RabbitMQ, we typically return immediately after publishing
            // The actual processing happens in a consumer
            // If you need to wait for the result, you can use RabbitMQ RPC pattern
            return CompletableFuture.completedFuture(
                    NotificationResponse.builder()
                            .success(true)
                            .messageId("rabbitmq-" + System.currentTimeMillis())
                            .provider("rabbitmq")
                            .notificationType(request.getNotificationType())
                            .message("Notification published to RabbitMQ queue")
                            .build()
            );
            
        } catch (Exception e) {
            log.error("Failed to publish notification request to RabbitMQ", e);
            return CompletableFuture.completedFuture(
                    NotificationResponse.failure(
                            "Failed to publish to RabbitMQ: " + e.getMessage(),
                            "rabbitmq",
                            request.getNotificationType()
                    )
            );
        }
    }

    @Override
    public String getPublisherType() {
        return "rabbitmq";
    }
}
```

#### Step 3: Create RabbitMQ Consumer (Optional but Recommended)

If you want to process notifications from RabbitMQ queue, create a consumer:

```java
package com.fractal.notify.async.impl;

import com.fractal.notify.core.NotificationRequest;
import com.fractal.notify.core.NotificationResponse;
import com.fractal.notify.core.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ consumer for processing notification requests from the queue.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQNotificationConsumer {
    private final NotificationService notificationService;

    @RabbitListener(queues = "${fractal.notify.async.rabbitmq.queue:notifications}")
    public void processNotification(NotificationRequest request) {
        try {
            log.debug("Processing notification from RabbitMQ queue: {}", request.getNotificationType());
            
            // Process the notification synchronously
            NotificationResponse response = notificationService.send(request);
            
            if (response.isSuccess()) {
                log.info("Notification processed successfully from RabbitMQ. MessageId: {}", 
                        response.getMessageId());
            } else {
                log.error("Notification processing failed from RabbitMQ. Error: {}", 
                        response.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("Error processing notification from RabbitMQ queue", e);
        }
    }
}
```

#### Step 4: Add Configuration Properties

Add RabbitMQ configuration to `NotificationProperties.java`:

```java
@Data
@ConfigurationProperties(prefix = "fractal.notify")
public class NotificationProperties {
    // ... existing properties ...
    
    @Data
    public static class AsyncConfig {
        private String mode = "async";
        private boolean enabled = true;
        private int corePoolSize = 5;
        private int maxPoolSize = 10;
        private int queueCapacity = 100;
        private RabbitMQConfig rabbitmq = new RabbitMQConfig(); // Add this
    }
    
    @Data
    public static class RabbitMQConfig {
        private String exchange = "notifications.exchange";
        private String routingKey = "notifications.routing";
        private String queue = "notifications";
        private boolean durable = true;
        private boolean autoDelete = false;
    }
}
```

#### Step 5: Configure RabbitMQ in application.properties

```properties
# Async Publisher Configuration
fractal.notify.async.mode=rabbitmq

# RabbitMQ Configuration
fractal.notify.async.rabbitmq.exchange=notifications.exchange
fractal.notify.async.rabbitmq.routing-key=notifications.routing
fractal.notify.async.rabbitmq.queue=notifications
fractal.notify.async.rabbitmq.durable=true
fractal.notify.async.rabbitmq.auto-delete=false

# Spring RabbitMQ Configuration
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

#### Step 6: Configure RabbitMQ Queue and Exchange (Optional)

Create a configuration class to set up RabbitMQ infrastructure:

```java
package com.fractal.notify.config;

import com.fractal.notify.config.NotificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "fractal.notify.async", name = "mode", havingValue = "rabbitmq")
public class RabbitMQConfig {
    private final NotificationProperties properties;

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    @Bean
    public TopicExchange notificationExchange() {
        NotificationProperties.RabbitMQConfig rabbitmq = properties.getAsync().getRabbitmq();
        return ExchangeBuilder.topicExchange(rabbitmq.getExchange())
                .durable(rabbitmq.isDurable())
                .build();
    }

    @Bean
    public Queue notificationQueue() {
        NotificationProperties.RabbitMQConfig rabbitmq = properties.getAsync().getRabbitmq();
        return QueueBuilder.durable(rabbitmq.getQueue())
                .build();
    }

    @Bean
    public Binding notificationBinding() {
        NotificationProperties.RabbitMQConfig rabbitmq = properties.getAsync().getRabbitmq();
        return BindingBuilder
                .bind(notificationQueue())
                .to(notificationExchange())
                .with(rabbitmq.getRoutingKey());
    }
}
```

---

## Alternative: Redis Publisher Example

Here's a quick example for Redis:

```java
package com.fractal.notify.async.impl;

import com.fractal.notify.async.AsyncNotificationPublisher;
import com.fractal.notify.config.NotificationProperties;
import com.fractal.notify.core.NotificationRequest;
import com.fractal.notify.core.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnClass(RedisTemplate.class)
@ConditionalOnProperty(prefix = "fractal.notify.async", name = "mode", havingValue = "redis")
public class RedisNotificationPublisher implements AsyncNotificationPublisher {
    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationProperties properties;

    @Override
    public CompletableFuture<NotificationResponse> publishAsync(NotificationRequest request) {
        String queueName = properties.getAsync().getRedis().getQueueName();
        
        try {
            // Push to Redis list (queue)
            redisTemplate.opsForList().rightPush(queueName, request);
            
            log.debug("Notification request published to Redis queue: {}", queueName);
            
            return CompletableFuture.completedFuture(
                    NotificationResponse.builder()
                            .success(true)
                            .messageId("redis-" + System.currentTimeMillis())
                            .provider("redis")
                            .notificationType(request.getNotificationType())
                            .message("Notification published to Redis queue")
                            .build()
            );
            
        } catch (Exception e) {
            log.error("Failed to publish notification to Redis", e);
            return CompletableFuture.completedFuture(
                    NotificationResponse.failure(
                            "Failed to publish to Redis: " + e.getMessage(),
                            "redis",
                            request.getNotificationType()
                    )
            );
        }
    }

    @Override
    public String getPublisherType() {
        return "redis";
    }
}
```

---

## Key Points to Remember

1. ✅ **Implement AsyncNotificationPublisher** - The interface with `publishAsync()` and `getPublisherType()` methods
2. ✅ **Use `@Component`** - So Spring will auto-detect it
3. ✅ **Use Conditional Annotations** - `@ConditionalOnProperty` to enable based on config
4. ✅ **Use `@ConditionalOnClass`** - To only load when required dependencies are present
5. ✅ **Return CompletableFuture** - Always return a `CompletableFuture<NotificationResponse>`
6. ✅ **Handle Errors Gracefully** - Return failure response instead of throwing exceptions
7. ✅ **Add Configuration Properties** - If your publisher needs configuration
8. ✅ **The system automatically uses it** - Based on `fractal.notify.async.mode` configuration

---

## How It Works

1. **Configuration-Based Selection**: The system selects the publisher based on `fractal.notify.async.mode`
2. **Spring Auto-Discovery**: Your `@Component` annotated publisher is automatically discovered
3. **Conditional Loading**: Only the configured publisher is loaded (others are ignored)
4. **Unified Interface**: All publishers implement the same interface, so switching is seamless

---

## Testing Your Publisher

After adding your publisher, test it:

```java
// In your application
@Autowired
private NotificationUtils notificationUtils;

public void testRabbitMQPublisher() {
    notificationUtils.email()
        .to("test@example.com")
        .subject("Test")
        .body("Hello from RabbitMQ!")
        .send(); // This will use RabbitMQ if configured
}
```

Or check the publisher type:

```java
@Autowired
private AsyncNotificationPublisher publisher;

public void checkPublisher() {
    System.out.println("Current publisher: " + publisher.getPublisherType());
    // Should output: "rabbitmq" if RabbitMQ is configured
}
```

---

## Summary Checklist

- [ ] Add required dependencies to `pom.xml`
- [ ] Create publisher class implementing `AsyncNotificationPublisher`
- [ ] Add `@Component` annotation
- [ ] Add `@ConditionalOnProperty` to enable based on config
- [ ] Add `@ConditionalOnClass` to check for required dependencies
- [ ] Implement `publishAsync()` method
- [ ] Implement `getPublisherType()` method
- [ ] Add configuration properties (if needed)
- [ ] Update `application.properties` with publisher mode
- [ ] Create consumer/listener (if needed for processing)
- [ ] Configure message broker infrastructure (queues, exchanges, etc.)

---

## Existing Publisher Examples

For reference, you can look at:

- **Default Async Publisher**: `AsyncNotificationPublisherImpl` in `com.fractal.notify.async.impl`
  - Uses Spring `@Async` for simple async processing
  - Default when `mode=async` or not specified

---

## Publisher vs Queue Mode

**Async Publisher** = Processes notifications asynchronously using a message broker or thread pool
- Examples: `@Async`, RabbitMQ, Redis, AWS SQS
- Notifications are processed immediately but asynchronously
- Good for: High throughput, distributed systems

**Queue Mode** = Database-backed queue with scheduled processing
- Notifications are persisted to database first
- A scheduler polls the database and processes notifications
- Good for: Reliability, crash recovery, preventing duplicates

Both can be used together or separately based on your needs!

---

## Advanced: RPC Pattern with RabbitMQ

If you need to wait for the notification result, you can use RabbitMQ RPC pattern:

```java
@Override
public CompletableFuture<NotificationResponse> publishAsync(NotificationRequest request) {
    try {
        // Send and wait for response
        NotificationResponse response = (NotificationResponse) rabbitTemplate
                .convertSendAndReceive(exchange, routingKey, request);
        
        return CompletableFuture.completedFuture(response);
    } catch (Exception e) {
        // Handle error
    }
}
```

---

No manual wiring or registration needed - it's all automatic! 🎉
