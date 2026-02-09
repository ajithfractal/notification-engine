# Adding a New Notification Strategy Guide

This guide explains how to add a completely new notification type (e.g., Push Notification, Slack, Discord) to the Fractal Notify system.

## Overview

The system uses a **Strategy Pattern** to support different notification types. To add a new notification type, you need to:
1. Add the notification type to the enum
2. Create the strategy package structure
3. Create DTOs (Request/Response)
4. Create the Provider interface and implementation
5. Create the ProviderFactory
6. Create the NotificationStrategy
7. Add builder to NotificationUtils (optional)
8. Add configuration properties

---

## Step-by-Step Guide

### Example: Adding a Push Notification Strategy

#### Step 1: Add Notification Type to Enum

First, add your new notification type to `NotificationType.java`:

```java
package com.fractal.notify.core;

public enum NotificationType {
    EMAIL,
    SMS,
    WHATSAPP,
    PUSH  // Add your new type here
}
```

#### Step 2: Create Package Structure

Create the following directory structure:
```
src/main/java/com/fractal/notify/push/
├── dto/
│   ├── PushRequest.java
│   └── PushResponse.java
├── provider/
│   └── FirebasePushProvider.java
├── PushNotificationStrategy.java
├── PushProvider.java
└── PushProviderFactory.java
```

#### Step 3: Create DTO Classes

**PushRequest.java:**
```java
package com.fractal.notify.push.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PushRequest {
    /**
     * Device token or user ID to send push notification to
     */
    private String to;
    
    /**
     * Title of the push notification
     */
    private String title;
    
    /**
     * Body/content of the push notification
     */
    private String body;
    
    /**
     * Additional data payload (optional)
     */
    private java.util.Map<String, String> data;
    
    /**
     * Priority level (high, normal)
     */
    private String priority;
}
```

**PushResponse.java:**
```java
package com.fractal.notify.push;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PushResponse {
    private boolean success;
    private String messageId;
    private String errorMessage;
}
```

#### Step 4: Create Provider Interface

**PushProvider.java:**
```java
package com.fractal.notify.push;

import com.fractal.notify.push.dto.PushRequest;

/**
 * Interface for push notification providers.
 * Different implementations can be created for Firebase, AWS SNS, etc.
 */
public interface PushProvider {
    /**
     * Send a push notification.
     *
     * @param request the push notification request
     * @return the push notification response
     */
    PushResponse sendPush(PushRequest request);

    /**
     * Get the provider name.
     *
     * @return the provider name
     */
    String getProviderName();
}
```

#### Step 5: Create Provider Implementation

**FirebasePushProvider.java:**
```java
package com.fractal.notify.push.provider;

import com.fractal.notify.config.NotificationProperties;
import com.fractal.notify.push.PushProvider;
import com.fractal.notify.push.PushResponse;
import com.fractal.notify.push.dto.PushRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirebasePushProvider implements PushProvider {
    private final NotificationProperties properties;
    private static final String PROVIDER_NAME = "firebase";
    
    // Add Firebase Admin SDK client here
    // private final FirebaseMessaging firebaseMessaging;

    @Override
    public PushResponse sendPush(PushRequest request) {
        try {
            if (!isConfigured()) {
                return PushResponse.builder()
                        .success(false)
                        .errorMessage("Firebase not configured")
                        .build();
            }
            
            // Implement Firebase push notification
            // Example:
            // Message message = Message.builder()
            //     .setToken(request.getTo())
            //     .setNotification(Notification.builder()
            //         .setTitle(request.getTitle())
            //         .setBody(request.getBody())
            //         .build())
            //     .putAllData(request.getData() != null ? request.getData() : Map.of())
            //     .build();
            // 
            // String messageId = firebaseMessaging.send(message);
            
            log.info("Push notification sent successfully via Firebase to {}", request.getTo());
            return PushResponse.builder()
                    .success(true)
                    .messageId("firebase-" + System.currentTimeMillis())
                    .build();
                    
        } catch (Exception e) {
            log.error("Error sending push notification via Firebase", e);
            return PushResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
    
    private boolean isConfigured() {
        // Check if Firebase is configured
        // return properties.getPush() != null 
        //         && properties.getPush().getFirebase() != null
        //         && properties.getPush().getFirebase().getServiceAccountKey() != null;
        return true; // Placeholder
    }
}
```

#### Step 6: Create Provider Factory

**PushProviderFactory.java:**
```java
package com.fractal.notify.push;

import com.fractal.notify.config.NotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for creating push notification providers based on configuration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushProviderFactory {
    private final List<PushProvider> pushProviders;
    private final NotificationProperties properties;

    /**
     * Get the configured push notification provider.
     *
     * @return the push notification provider
     */
    public PushProvider getProvider() {
        String providerName = properties.getPush().getProvider();
        log.debug("Getting push notification provider: {}", providerName);

        Map<String, PushProvider> providerMap = pushProviders.stream()
                .collect(Collectors.toMap(
                        PushProvider::getProviderName,
                        Function.identity()
                ));

        PushProvider provider = providerMap.get(providerName);
        if (provider == null) {
            log.warn("Push notification provider '{}' not found, using default Firebase", providerName);
            return providerMap.get("firebase");
        }

        return provider;
    }
}
```

#### Step 7: Create Notification Strategy

**PushNotificationStrategy.java:**
```java
package com.fractal.notify.push;

import com.fractal.notify.core.NotificationRequest;
import com.fractal.notify.core.NotificationResponse;
import com.fractal.notify.core.NotificationType;
import com.fractal.notify.core.NotificationStrategy;
import com.fractal.notify.push.dto.PushRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushNotificationStrategy implements NotificationStrategy {
    private final PushProviderFactory pushProviderFactory;

    @Override
    public NotificationResponse send(NotificationRequest request) {
        log.debug("Processing push notification to {}", request.getTo() != null ? request.getTo() : "N/A");

        PushProvider provider = pushProviderFactory.getProvider();
        
        // For push notifications, use the first recipient (device token)
        String recipient = (request.getTo() != null && !request.getTo().isEmpty()) 
                ? request.getTo().get(0) 
                : null;
        
        // Convert NotificationRequest to PushRequest
        // Note: You may need to extract title from subject or use a different mapping
        PushRequest pushRequest = PushRequest.builder()
                .to(recipient)
                .title(request.getSubject()) // Use subject as title
                .body(request.getBody())
                .data(request.getTemplateVariables()) // Map template variables to data
                .priority("high")
                .build();

        PushResponse pushResponse = provider.sendPush(pushRequest);

        if (pushResponse.isSuccess()) {
            return NotificationResponse.success(
                    pushResponse.getMessageId(),
                    provider.getProviderName(),
                    NotificationType.PUSH
            );
        } else {
            return NotificationResponse.failure(
                    pushResponse.getErrorMessage(),
                    provider.getProviderName(),
                    NotificationType.PUSH
            );
        }
    }

    @Override
    public NotificationType getType() {
        return NotificationType.PUSH;
    }
}
```

#### Step 8: Add Configuration Properties (Optional)

Add configuration to `NotificationProperties.java`:

```java
@Data
@ConfigurationProperties(prefix = "fractal.notify")
public class NotificationProperties {
    // ... existing properties ...
    private PushConfig push = new PushConfig();
    
    @Data
    public static class PushConfig {
        private String provider = "firebase";
        private FirebaseConfig firebase = new FirebaseConfig();
    }
    
    @Data
    public static class FirebaseConfig {
        private String serviceAccountKey;
        private String projectId;
    }
}
```

#### Step 9: Configure in application.properties

```properties
fractal.notify.push.provider=firebase
fractal.notify.push.firebase.service-account-key=path/to/service-account-key.json
fractal.notify.push.firebase.project-id=your-project-id
```

#### Step 10: Add Builder to NotificationUtils (Optional but Recommended)

Add a builder method to `NotificationUtils.java`:

```java
/**
 * Create a push notification builder.
 *
 * @return PushNotificationBuilder
 */
public PushNotificationBuilder push() {
    PushNotificationBuilder builder = new PushNotificationBuilder(NotificationType.PUSH);
    builder.setUtils(this);
    return builder;
}
```

And create the builder class:

```java
/**
 * Builder for push notifications.
 */
public static class PushNotificationBuilder extends NotificationBuilder {
    protected PushNotificationBuilder(NotificationType type) {
        super(type);
    }
    
    // Add any push-specific methods here if needed
    // For example:
    // public PushNotificationBuilder priority(String priority) {
    //     // Set priority
    //     return this;
    // }
}
```

---

## Key Points to Remember

1. ✅ **Add to NotificationType enum** - This is required for the system to recognize your strategy
2. ✅ **Use `@Component` on Strategy** - So Spring will auto-detect it
3. ✅ **Implement NotificationStrategy interface** - With `send()` and `getType()` methods
4. ✅ **Create Provider interface and implementation** - Follow the provider pattern
5. ✅ **Create ProviderFactory** - To select the correct provider based on config
6. ✅ **Map NotificationRequest to your DTO** - Convert the generic request to your specific format
7. ✅ **Return NotificationResponse** - Use `NotificationResponse.success()` or `failure()` helper methods
8. ✅ **Add configuration properties** - If your strategy needs configuration
9. ✅ **The system automatically discovers it** - No manual registration needed!

---

## Testing Your Strategy

After adding your strategy, test it:

```java
// In your application
@Autowired
private NotificationUtils notificationUtils;

public void testPushNotification() {
    notificationUtils.push()
        .to("device-token-123")
        .subject("New Message")
        .body("You have a new message!")
        .send();
}
```

Or use the service directly:

```java
@Autowired
private NotificationService notificationService;

public void testPushNotification() {
    NotificationRequest request = NotificationRequest.builder()
        .notificationType(NotificationType.PUSH)
        .to(List.of("device-token-123"))
        .subject("New Message")
        .body("You have a new message!")
        .build();
    
    NotificationResponse response = notificationService.send(request);
}
```

---

## Summary Checklist

- [ ] Add new type to `NotificationType` enum
- [ ] Create package structure (`dto/`, `provider/`)
- [ ] Create DTO classes (`*Request.java`, `*Response.java`)
- [ ] Create Provider interface
- [ ] Create at least one Provider implementation
- [ ] Create ProviderFactory
- [ ] Create NotificationStrategy implementation
- [ ] Add `@Component` annotation to Strategy
- [ ] Implement `send()` method (map NotificationRequest → your DTO → provider)
- [ ] Implement `getType()` method (return your NotificationType)
- [ ] Add configuration properties (if needed)
- [ ] Update `application.properties` (if needed)
- [ ] Add builder to NotificationUtils (optional but recommended)
- [ ] Add any required dependencies to `pom.xml`

---

## How It Works

1. **Spring Auto-Discovery**: Your `@Component` annotated strategy is automatically discovered by Spring
2. **Strategy Pattern**: `NotificationService` collects all strategies via dependency injection
3. **Type-Based Selection**: The service finds the strategy by matching `NotificationType`
4. **Provider Pattern**: The strategy uses its factory to get the correct provider
5. **Unified API**: All notification types use the same `NotificationRequest`/`NotificationResponse` interface

---

## Existing Strategy Examples

For reference, you can look at existing implementations:

- **Email**: `EmailNotificationStrategy` in `com.fractal.notify.email`
- **SMS**: `SMSNotificationStrategy` in `com.fractal.notify.sms`
- **WhatsApp**: `WhatsAppNotificationStrategy` in `com.fractal.notify.whatsapp`

These serve as excellent templates for creating your own strategy implementations.

---

## Differences: Strategy vs Provider

**Strategy** = A new notification type/channel (Email, SMS, Push, etc.)
- Requires adding to `NotificationType` enum
- Requires creating a new strategy class
- Requires creating provider interface and factory
- Example: Adding Push Notifications as a new channel

**Provider** = A different implementation of an existing notification type
- Only requires creating a new provider implementation
- Example: Adding SendGrid as an alternative to SMTP for emails

See `ADDING_NEW_PROVIDER.md` for adding providers to existing strategies.

---

## Advanced: Custom Request Mapping

If your notification type needs special handling beyond the standard `NotificationRequest` fields, you can:

1. **Use template variables** - Store custom data in `templateVariables` map
2. **Extend NotificationRequest** - Add custom fields (requires modifying core)
3. **Use subject/body creatively** - Map fields as needed in your strategy

Example mapping in strategy:
```java
PushRequest pushRequest = PushRequest.builder()
    .to(recipient)
    .title(request.getSubject())
    .body(request.getBody())
    .data(extractCustomData(request.getTemplateVariables()))
    .priority(determinePriority(request))
    .build();
```

---

No manual wiring or registration needed - it's all automatic! 🎉
