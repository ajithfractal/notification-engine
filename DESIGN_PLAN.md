# Fractal Notification Engine - Design Plan

## Executive Summary

A reusable notification library that provides a unified interface for sending notifications (Email, SMS, WhatsApp) across multiple Spring Boot applications. Designed with a simple start and easy scalability path.

## Current Architecture

### Simple & Efficient Design

- **Single Library**: One dependency to add to any Spring Boot application
- **Java Built-in Async**: Uses `@Async` with thread pool (no external infrastructure)
- **Clean API**: Simple `NotificationUtils` class for easy usage
- **Provider Agnostic**: Switch providers via configuration

### Architecture Flow

```
Client Application
    ↓
NotificationUtils (Simple API)
    ↓
NotificationService
    ↓
AsyncNotificationPublisher (Interface)
    ↓
AsyncNotificationPublisherImpl (@Async - Current)
    ↓
NotificationStrategy → Provider → External Service
```

## Key Design Decision: Abstraction Layer

### AsyncNotificationPublisher Interface

This interface allows switching async mechanisms without changing client code:

**Current Implementation**:
- `AsyncNotificationPublisherImpl` - Uses Spring `@Async`
- Simple, no infrastructure needed
- Good for low to medium volume

**Future Implementation** (Already Built):
- `KafkaNotificationPublisher` - Uses Kafka
- Enabled when `fractal.notify.async.mode=kafka`
- For high throughput scenarios

**Benefit**: Zero code changes when migrating to Kafka

## Migration Path

### When Throughput Increases

1. **Add Kafka** (infrastructure)
2. **Change one config line**: `fractal.notify.async.mode=kafka`
3. **Deploy consumer service** (separate app)
4. **Done!** No client code changes

## Components

### What's Included

1. **NotificationUtils** - Simple API for clients
2. **NotificationService** - Core processing logic
3. **Strategies** - Email, SMS, WhatsApp handlers
4. **Providers** - SMTP, Twilio implementations
5. **Template Engine** - Thymeleaf-based templating
6. **Async Abstraction** - Interface for async processing

### What's Optional (Future)

- Kafka support (already implemented, just needs to be enabled)
- Audit & Pricing (to be added later)

## Usage Example

```java
@Autowired
NotificationUtils notificationUtils;

// Simple usage
notificationUtils.email()
    .to("user@example.com")
    .subject("Welcome")
    .template("welcome")
    .variable("name", "John")
    .send();
```

## Benefits

1. **Simple Start**: No complex infrastructure, just add dependency
2. **Future-Ready**: Easy migration to Kafka when needed
3. **No Breaking Changes**: Client code stays the same
4. **Provider Flexibility**: Switch providers via config
5. **Clean API**: Easy to use, minimal learning curve

## Technical Highlights

- **Design Pattern**: Strategy + Provider + Factory patterns
- **Abstraction**: `AsyncNotificationPublisher` interface for flexibility
- **Extensibility**: Easy to add new notification types or providers
- **Configuration-Driven**: All behavior controlled via YAML/properties

## Next Steps

1. **Phase 1** (Current): Simple async with @Async ✅
2. **Phase 2** (When Needed): Enable Kafka support
3. **Phase 3** (Future): Add audit & pricing features

---

**Status**: Ready for implementation  
**Complexity**: Low (simple async) → Medium (with Kafka)  
**Scalability**: High (ready for Kafka migration)
