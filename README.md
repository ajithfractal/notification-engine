# Fractal Notification Engine
## Architecture & Design Document

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Architecture Overview](#architecture-overview)
3. [Design Patterns](#design-patterns)
4. [System Components](#system-components)
5. [Auditing & Pricing](#auditing--pricing)
6. [Configuration Guide](#configuration-guide)
7. [Extensibility](#extensibility)

---

## Executive Summary

The **Fractal Notification Engine** is a reusable notification library that provides a unified interface for sending notifications across multiple channels (Email, SMS, WhatsApp). Built with extensibility in mind, it allows easy swapping of notification providers without code changes.

**Current Implementation**: Uses Java built-in async processing (@Async) for simplicity and ease of use.

**Future-Ready Architecture**: Designed with abstraction layers that allow seamless migration to Kafka when throughput increases, without changing client code.

### Key Features

- **Multi-Channel Support**: Email, SMS, WhatsApp (extensible)
- **Provider Agnostic**: Switch providers via configuration (e.g., Twilio → AWS SNS)
- **Template Engine**: Thymeleaf-based templating for dynamic content
- **Async Processing**: Java built-in async (@Async) - simple and efficient
- **Kafka-Ready**: Abstraction layer allows easy migration to Kafka when needed
- **Audit & Pricing**: PostgreSQL-based tracking with automatic cost calculation (future)
- **Simple API**: Clean `NotificationUtils` API for easy integration

---

## Architecture Overview

### Current Architecture (Simple & Efficient)

The notification engine is designed as a **single library** that can be easily added to any Spring Boot application. It uses Java built-in async processing for simplicity.

**Current Flow**:
```
Client Application
    ↓
NotificationUtils
    ↓
NotificationService
    ↓
AsyncNotificationPublisher (Interface)
    ↓
AsyncNotificationPublisherImpl (@Async)
    ↓
NotificationStrategy → Provider → External Service
```

### Future Architecture (When Throughput Increases)

When you need higher throughput, the same code can be extended to use Kafka without changing client code:

**Future Flow**:
```
Client Application
    ↓
NotificationUtils (Same API)
    ↓
NotificationService (Same)
    ↓
AsyncNotificationPublisher (Interface - Same)
    ↓
KafkaNotificationPublisher (New Implementation)
    ↓
Kafka Topic
    ↓
Consumer Service (Separate App)
    ↓
NotificationStrategy → Provider → External Service
```

### Key Design: Abstraction Layer

The `AsyncNotificationPublisher` interface allows switching implementations:

- **Current**: `AsyncNotificationPublisherImpl` - Uses @Async
- **Future**: `KafkaNotificationPublisher` - Uses Kafka (already implemented, just needs to be enabled)

**Benefits**:
1. **No Code Changes**: Client code remains the same when switching to Kafka
2. **Easy Migration**: Just change configuration: `fractal.notify.async.mode=kafka`
3. **Gradual Adoption**: Start simple, scale when needed
4. **Future-Proof**: Architecture ready for high throughput scenarios

---

## Design Patterns

### 1. Strategy Pattern

**Purpose**: Encapsulate different notification algorithms (Email, SMS, WhatsApp) and make them interchangeable.

**Benefits**:
- Easy to add new notification types
- Client code doesn't need to know specific implementation
- Each strategy can have its own logic

### 2. Provider Pattern

**Purpose**: Allow multiple implementations for the same notification type (e.g., Twilio SMS vs AWS SNS SMS).

**Benefits**:
- Swap providers via configuration (no code changes)
- Test with mock providers
- Support multiple providers simultaneously

### 3. Factory Pattern

**Purpose**: Create appropriate provider instances based on configuration.

```
Configuration → ProviderFactory → Provider Instance
```

---

## System Components

### Core Components

#### 1. NotificationUtils
- **Role**: Main API for client applications
- **Responsibilities**:
  - Provide simple, clean API for sending notifications
  - Builder pattern for easy usage
  - Validate notification requests

**Usage**:
```java
@Autowired
NotificationUtils notificationUtils;

notificationUtils.email()
    .to("user@example.com")
    .subject("Welcome")
    .template("welcome")
    .variable("name", "John")
    .send();
```

#### 2. NotificationService
- **Role**: Core service for processing notifications
- **Responsibilities**:
  - Route requests to appropriate strategy
  - Handle template rendering
  - Coordinate async publishing

#### 3. AsyncNotificationPublisher (Abstraction Layer)
- **Role**: Interface for async notification publishing
- **Current Implementation**: `AsyncNotificationPublisherImpl` (uses @Async)
- **Future Implementation**: `KafkaNotificationPublisher` (uses Kafka)
- **Benefit**: Easy migration to Kafka without changing client code

#### 4. NotificationStrategy Implementations
- **EmailNotificationStrategy**: Handles email notifications
- **SMSNotificationStrategy**: Handles SMS notifications
- **WhatsAppNotificationStrategy**: Handles WhatsApp notifications

#### 5. Provider Implementations
- **EmailProvider**: SMTPEmailProvider, SendGridEmailProvider
- **SMSProvider**: TwilioSMSProvider, AWSSNSProvider
- **WhatsAppProvider**: (Future implementations)

#### 6. TemplateService
- **Technology**: Thymeleaf
- **Purpose**: Render dynamic templates with variables
- **Location**: `resources/templates/{type}/{templateName}.html`

#### 7. NotificationAuditService (Future)
- **Role**: Audit logging service
- **Responsibilities**:
  - Record all notification attempts
  - Calculate costs via PricingService
  - Store audit records in PostgreSQL

#### 8. PricingService (Future)
- **Role**: Cost calculation engine
- **Calculation Rules**:
  - SMS: Fixed cost per message (provider-specific)
  - Email: Cost per KB of content
  - WhatsApp: Fixed cost per message

---

## Auditing & Pricing

### Audit Entity Structure

```sql
NOTIFICATION_AUDIT {
    bigserial id PK
    varchar notification_type
    varchar provider
    varchar recipient
    varchar status
    decimal cost
    varchar currency
    integer content_length
    jsonb metadata
    text error_message
    timestamp created_at
}
```

### Database Schema

```sql
CREATE TABLE notification_audit (
    id BIGSERIAL PRIMARY KEY,
    notification_type VARCHAR(50) NOT NULL,
    provider VARCHAR(100) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL, -- SUCCESS, FAILED
    cost DECIMAL(10, 6) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    content_length INTEGER,
    metadata JSONB,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient queries
CREATE INDEX idx_created_at ON notification_audit(created_at);
CREATE INDEX idx_notification_type ON notification_audit(notification_type);
CREATE INDEX idx_provider ON notification_audit(provider);
CREATE INDEX idx_status ON notification_audit(status);
```

### Pricing Examples

| Notification Type | Provider | Pricing Model | Example Cost |
|------------------|----------|---------------|--------------|
| SMS | Twilio | $0.005 per message | 100 SMS = $0.50 |
| SMS | AWS SNS | $0.006 per message | 100 SMS = $0.60 |
| Email | SMTP | $0.0001 per KB | 5 KB email = $0.0005 |
| Email | SendGrid | $0.0002 per KB | 5 KB email = $0.001 |
| WhatsApp | Default | $0.01 per message | 100 messages = $1.00 |

---

## Configuration Guide

### Application Configuration (application.yml)

```yaml
fractal:
  notify:
    enabled: true
    async:
      mode: async  # Options: async (current), kafka (for future use)
      enabled: true
      core-pool-size: 5
      max-pool-size: 10
      queue-capacity: 100
      # Kafka configuration (for future use when throughput increases)
      kafka:
        enabled: false
        topic: notifications
        consumer-group: notification-processors
    
    # Email Configuration
    email:
      provider: smtp  # Options: smtp, sendgrid
      smtp:
        host: smtp.gmail.com
        port: 587
        username: ${EMAIL_USERNAME}
        password: ${EMAIL_PASSWORD}
        from: noreply@company.com
    
    # SMS Configuration
    sms:
      provider: twilio  # Options: twilio, aws-sns
      twilio:
        account-sid: ${TWILIO_ACCOUNT_SID}
        auth-token: ${TWILIO_AUTH_TOKEN}
        from-number: ${TWILIO_FROM_NUMBER}
    
    # WhatsApp Configuration (Future)
    whatsapp:
      provider: default
      enabled: false
```

### Migrating to Kafka (When Needed)

When throughput increases and you need Kafka, simply:

1. **Add Kafka dependency** (if not already present)
2. **Update configuration**:
```yaml
fractal:
  notify:
    async:
      mode: kafka  # Change from 'async' to 'kafka'
      kafka:
        enabled: true
        topic: notifications

spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
```

3. **No code changes needed!** The `AsyncNotificationPublisher` interface handles the switch automatically.

### Switching Providers

To switch from Twilio to AWS SNS:

```yaml
fractal:
  notify:
    sms:
      provider: aws-sns  # Changed from 'twilio'
    pricing:
      sms:
        aws-sns: 0.006   # Update pricing
```

**No code changes required!**

---

## Extensibility

### Adding a New Notification Type

1. Create strategy class implementing `NotificationStrategy`
2. Create provider interface (e.g., `PushNotificationProvider`)
3. Implement provider(s) (e.g., `FCMProvider`, `APNSProvider`)
4. Register in `NotificationProviderFactory`
5. Add pricing configuration

### Adding a New Provider

1. Implement the provider interface (e.g., `SMSProvider`)
2. Register in `NotificationProviderFactory`
3. Add configuration properties
4. Add pricing configuration

**Example**: Adding AWS SNS SMS Provider

```java
@Component
public class AWSSNSProvider implements SMSProvider {
    @Override
    public SMSResponse sendSMS(SMSRequest request) {
        // AWS SNS implementation
    }
}
```

```yaml
fractal:
  notify:
    sms:
      provider: aws-sns  # Now available
    pricing:
      sms:
        aws-sns: 0.006
```

---

## Key Benefits

### 1. Architecture Benefits
- **Simple Current Design**: Uses Java built-in async, no infrastructure needed
- **Future-Ready**: Easy migration to Kafka when throughput increases
- **Abstraction Layer**: `AsyncNotificationPublisher` interface allows switching implementations
- **No Code Changes**: Client code remains the same when migrating to Kafka

### 2. Provider Flexibility
- Switch providers via configuration
- No code changes required
- Support multiple providers simultaneously

### 3. Cost Transparency
- Automatic cost calculation
- Historical audit trail
- Cost analysis by type/provider

### 4. Extensibility
- Add new notification types easily
- Add new providers without modifying existing code
- Follows Open/Closed Principle

### 5. Production Ready
- Kafka-based async processing for reliability
- Comprehensive error handling
- Audit trail for compliance
- Message persistence (survives failures)

### 6. Developer Experience
- Simple, clean API (`NotificationUtils`)
- Auto-configuration
- Template support for dynamic content
- Minimal configuration in client apps

---

## Future Enhancements

1. **Retry Mechanism**: Automatic retry for failed notifications
2. **Rate Limiting**: Cost-based rate limiting
3. **Analytics Dashboard**: REST endpoints for cost reporting
4. **Delivery Status Tracking**: Track delivery status and update audit records
5. **Webhook Support**: Provider webhook integration for delivery status
6. **Multi-tenancy**: Support for multiple organizations with separate billing

---

## Migration Path to Kafka

### Current State (Simple)

- Uses `@Async` with thread pool
- All processing happens in the same application
- Simple, no infrastructure needed
- Good for low to medium volume

### When to Migrate to Kafka

Consider migrating when:
- Notification volume > 1000/minute
- Need message persistence
- Need to decouple producer/consumer
- Need horizontal scaling of processing
- Need fault tolerance (messages survive crashes)

### How to Migrate

1. **Add Kafka dependency** (already optional in pom.xml)
2. **Start Kafka cluster**
3. **Update configuration**:
   ```yaml
   fractal:
     notify:
       async:
         mode: kafka  # Change from 'async'
   ```
4. **Deploy consumer service** (separate application)
5. **No client code changes needed!**

The `AsyncNotificationPublisher` interface ensures seamless migration.

## Conclusion

The Fractal Notification Engine provides a robust, extensible solution for multi-channel notifications. The current implementation uses Java built-in async processing for simplicity, while the abstraction layer (`AsyncNotificationPublisher`) allows seamless migration to Kafka when throughput increases. This design ensures:

- **Simple Start**: Easy to integrate, no complex infrastructure
- **Future Growth**: Ready to scale with Kafka when needed
- **No Breaking Changes**: Migration to Kafka doesn't require client code changes
- **Provider Flexibility**: Easy to swap providers via configuration

The architecture is ideal for production environments where requirements may change over time, allowing teams to start simple and scale as needed.

---

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>com.fractal</groupId>
    <artifactId>fractal-notify</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Configure Application

Add configuration to your `application.yml`:

```yaml
fractal:
  notify:
    enabled: true
    email:
      smtp:
        host: smtp.gmail.com
        username: ${EMAIL_USERNAME}
        password: ${EMAIL_PASSWORD}
    sms:
      twilio:
        account-sid: ${TWILIO_ACCOUNT_SID}
        auth-token: ${TWILIO_AUTH_TOKEN}
```

### 3. Use NotificationUtils

```java
@Autowired
private NotificationUtils notificationUtils;

public void sendWelcomeEmail(String email, String name) {
    // Simple, clean API
    notificationUtils.email()
        .to(email)
        .subject("Welcome to Our Platform")
        .template("welcome")
        .variable("name", name)
        .send();
}

// Or use builder pattern
public void sendOTP(String phoneNumber, String otp) {
    notificationUtils.sms()
        .to(phoneNumber)
        .body("Your OTP is: " + otp)
        .send();
}
```

### 4. Enable Component Scanning

Make sure your Spring Boot application scans the package:

```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.fractal.notify", "your.package"})
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

**That's it!** The library uses Java built-in async processing by default. No Kafka setup needed.

---

**Document Version**: 1.0  
**Last Updated**: 2024  
**Author**: Fractal Engineering Team
