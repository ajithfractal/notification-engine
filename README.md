# Fractal Notification Engine

A reusable Spring Boot notification library for sending multi-channel notifications (Email, SMS, WhatsApp) with PostgreSQL persistence, client-provided templates, and advanced email features.

---

## Table of Contents

1. [Features](#features)
2. [Quick Start](#quick-start)
3. [Configuration](#configuration)
4. [Usage Examples](#usage-examples)
5. [Architecture](#architecture)
6. [Database Schema](#database-schema)
7. [Extensibility](#extensibility)

---

## Features

- **Multi-Channel Support**: Email, SMS, WhatsApp (extensible)
- **Multiple Recipients**: Support for multiple TO, CC, and BCC recipients
- **Client-Provided Templates**: Pass template content directly or use library templates
- **PostgreSQL Persistence**: Automatic persistence with status tracking (PENDING, PROCESSING, SENT, FAILED, RETRYING)
- **Database Queue**: Optional queue-based processing with scheduler (prevents duplicate emails)
- **Crash Recovery**: Notifications persist before sending, survive system crashes
- **Automatic Retry**: Configurable retry mechanism for failed notifications
- **Duplicate Prevention**: Prevents sending duplicate emails within a time window
- **Provider Agnostic**: Switch providers via configuration (e.g., Twilio → AWS SNS)
- **Async Processing**: Java built-in async (@Async) - simple and efficient
- **Extensible Async Publishers**: Easy to add RabbitMQ, Redis, or other message brokers
- **Simple API**: Clean `NotificationUtils` builder pattern for easy integration

---

## Quick Start

### 1. Add Dependency

**Maven:**
```xml
<dependency>
    <groupId>com.fractal</groupId>
    <artifactId>fractal-notify</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Gradle:**
```gradle
dependencies {
    implementation 'com.fractal:fractal-notify:1.0.0'
}
```

### 2. Configure Application

Create `application.properties`:

```properties
# Enable notification engine
fractal.notify.enabled=true

# Async Configuration
fractal.notify.async.mode=async
fractal.notify.async.core-pool-size=5
fractal.notify.async.max-pool-size=10
fractal.notify.async.queue-capacity=100

# Email Configuration
fractal.notify.email.provider=smtp
fractal.notify.email.smtp.host=${EMAIL_HOST:smtp.gmail.com}
fractal.notify.email.smtp.port=${EMAIL_PORT:587}
fractal.notify.email.smtp.username=${EMAIL_USERNAME}
fractal.notify.email.smtp.password=${EMAIL_PASSWORD}
fractal.notify.email.smtp.from=${EMAIL_FROM:noreply@company.com}

# SMS Configuration
fractal.notify.sms.provider=twilio
fractal.notify.sms.twilio.account-sid=${TWILIO_ACCOUNT_SID}
fractal.notify.sms.twilio.auth-token=${TWILIO_AUTH_TOKEN}
fractal.notify.sms.twilio.from-number=${TWILIO_FROM_NUMBER}

# Persistence Configuration (PostgreSQL)
fractal.notify.persistence.enabled=true
fractal.notify.persistence.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/notifications}
fractal.notify.persistence.datasource.username=${DB_USERNAME:postgres}
fractal.notify.persistence.datasource.password=${DB_PASSWORD:postgres}
fractal.notify.persistence.datasource.driver-class-name=org.postgresql.Driver

# Spring Data JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

### 3. Enable Component Scanning

```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.fractal.notify", "your.package"})
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

### 4. Use It!

```java
@Autowired
private NotificationUtils notificationUtils;

// Send email with single recipient
notificationUtils.email()
    .to("user@example.com")
    .subject("Welcome")
    .body("Welcome to our platform!")
    .send();

// Send email with multiple recipients, CC, and BCC
notificationUtils.email()
    .to("user1@example.com", "user2@example.com")
    .cc("manager@example.com")
    .bcc("archive@example.com")
    .subject("Team Update")
    .body("Important update for the team")
    .send();
```

---

## Configuration

### Complete Configuration Reference

```properties
# Notification Engine
fractal.notify.enabled=true

# Async Processing
fractal.notify.async.mode=async
fractal.notify.async.enabled=true
fractal.notify.async.core-pool-size=5
fractal.notify.async.max-pool-size=10
fractal.notify.async.queue-capacity=100

# Email
fractal.notify.email.provider=smtp
fractal.notify.email.smtp.host=smtp.gmail.com
fractal.notify.email.smtp.port=587
fractal.notify.email.smtp.username=${EMAIL_USERNAME}
fractal.notify.email.smtp.password=${EMAIL_PASSWORD}
fractal.notify.email.smtp.from=${EMAIL_FROM}

# SMS
fractal.notify.sms.provider=twilio
fractal.notify.sms.twilio.account-sid=${TWILIO_ACCOUNT_SID}
fractal.notify.sms.twilio.auth-token=${TWILIO_AUTH_TOKEN}
fractal.notify.sms.twilio.from-number=${TWILIO_FROM_NUMBER}

# WhatsApp (Future)
fractal.notify.whatsapp.provider=default
fractal.notify.whatsapp.enabled=false

# Persistence
fractal.notify.persistence.enabled=true
fractal.notify.persistence.datasource.url=jdbc:postgresql://localhost:5432/notifications
fractal.notify.persistence.datasource.username=${DB_USERNAME}
fractal.notify.persistence.datasource.password=${DB_PASSWORD}
fractal.notify.persistence.datasource.driver-class-name=org.postgresql.Driver
```

---

## Usage Examples

### Email Notifications

#### Basic Email
```java
notificationUtils.email()
    .to("user@example.com")
    .subject("Welcome")
    .body("Welcome to our platform!")
    .send();
```

#### Multiple Recipients with CC/BCC
```java
notificationUtils.email()
    .to("user1@example.com", "user2@example.com", "user3@example.com")
    .cc("manager@example.com")
    .bcc("archive@example.com")
    .subject("Team Meeting")
    .body("Meeting scheduled for tomorrow")
    .send();
```

#### Using Library Template
```java
notificationUtils.email()
    .to("user@example.com")
    .subject("Welcome")
    .template("welcome")  // Loads from resources/templates/email/welcome.html
    .variable("name", "John")
    .variable("company", "Acme Corp")
    .send();
```

#### Using Client-Provided Template
```java
String template = """
    <html>
        <body>
            <h1>Welcome, <span th:text="${name}">User</span>!</h1>
            <p>Your account has been created.</p>
        </body>
    </html>
    """;

notificationUtils.email()
    .to("user@example.com")
    .subject("Welcome")
    .templateContent(template)  // Client-provided template
    .variable("name", "John")
    .send();
```

#### Synchronous Sending
```java
NotificationResponse response = notificationUtils.email()
    .to("user@example.com")
    .subject("Test")
    .body("Test message")
    .sendSync();

if (response.isSuccess()) {
    System.out.println("Sent: " + response.getMessageId());
} else {
    System.out.println("Failed: " + response.getErrorMessage());
}
```

### SMS Notifications

```java
// Single recipient
notificationUtils.sms()
    .to("+1234567890")
    .body("Your OTP is: 123456")
    .send();

// Using list
List<String> recipients = Arrays.asList("+1234567890", "+0987654321");
notificationUtils.sms()
    .to(recipients)
    .body("Important alert")
    .send();
```

### WhatsApp Notifications

```java
notificationUtils.whatsapp()
    .to("+1234567890")
    .body("Hello from WhatsApp!")
    .send();
```

---

## Architecture

### Current Flow

```
Client Application
    ↓
NotificationUtils (Builder API)
    ↓
NotificationService
    ↓
NotificationPersistenceService (Persist to DB with PENDING status)
    ↓
AsyncNotificationPublisher (Interface)
    ↓
AsyncNotificationPublisherImpl (@Async)
    ↓
NotificationStrategy → Provider → External Service
    ↓
NotificationPersistenceService (Update status to SENT/FAILED)
```

### Key Components

1. **NotificationUtils**: Simple builder API for clients
2. **NotificationService**: Core processing and routing
3. **NotificationPersistenceService**: Database persistence and status tracking
4. **AsyncNotificationPublisher**: Abstraction for async processing
5. **NotificationStrategy**: Handles different notification types (Email, SMS, WhatsApp)
6. **Provider**: Actual implementation (SMTP, Twilio, etc.)
7. **TemplateService**: Renders templates (library or client-provided)

### Design Patterns

- **Strategy Pattern**: Different notification types (Email, SMS, WhatsApp)
- **Provider Pattern**: Multiple implementations per type (Twilio, AWS SNS, etc.)
- **Factory Pattern**: Creates providers based on configuration
- **Builder Pattern**: Fluent API in NotificationUtils

---

## Database Queue (Optional)

The notification engine supports an optional database queue mode where notifications are persisted to PostgreSQL and processed by a background scheduler. This provides better reliability, prevents duplicate emails, and handles system crashes gracefully.

### Queue Mode vs Direct Mode

**Direct Mode (Default)**: Notifications are sent immediately via async publisher
- Faster response time
- Simpler setup
- No scheduler overhead

**Queue Mode**: Notifications are queued in database and processed by scheduler
- Better reliability (survives crashes)
- Prevents duplicate emails
- Automatic retry mechanism
- Better for high-volume scenarios

### Enabling Queue Mode

Set `fractal.notify.queue.enabled=true` in your `application.properties`:

```properties
# Enable queue mode
fractal.notify.queue.enabled=true
fractal.notify.queue.poll-interval=5000        # Poll every 5 seconds
fractal.notify.queue.batch-size=10            # Process 10 notifications per batch
fractal.notify.queue.max-retries=3            # Retry failed notifications up to 3 times
fractal.notify.queue.retry-delay=60000        # Wait 60 seconds before retry
```

### Queue Flow

```
Client Application
    ↓
NotificationService.sendAsync()
    ↓
NotificationPersistenceService (Persist with PENDING status)
    ↓
Return immediately (notification queued)
    ↓
NotificationQueueProcessor (Scheduler - runs every poll-interval)
    ↓
Fetch PENDING notifications with pessimistic lock
    ↓
Mark as PROCESSING (prevents duplicate processing)
    ↓
Send notification
    ↓
Update status to SENT or FAILED
    ↓
If FAILED and retry count < max-retries: Mark as RETRYING
```

### Queue Features

1. **Pessimistic Locking**: Uses database locks to prevent multiple schedulers from processing the same notification
2. **Duplicate Prevention**: Checks for duplicate emails (same recipients, subject, body) within 1 hour window
3. **Automatic Retry**: Failed notifications are automatically retried with configurable delay
4. **Stuck Notification Recovery**: Notifications stuck in PROCESSING status (from crashes) are automatically recovered
5. **Batch Processing**: Processes multiple notifications per scheduler run for efficiency

### Notification Statuses in Queue Mode

- **PENDING**: Queued, waiting to be processed
- **PROCESSING**: Currently being processed by scheduler (locked)
- **SENT**: Successfully sent
- **FAILED**: Failed to send (exceeded max retries)
- **RETRYING**: Failed but will be retried

### Configuration Options

| Property | Default | Description |
|----------|---------|-------------|
| `fractal.notify.queue.enabled` | `false` | Enable/disable queue mode |
| `fractal.notify.queue.poll-interval` | `5000` | Poll interval in milliseconds |
| `fractal.notify.queue.batch-size` | `10` | Number of notifications to process per batch |
| `fractal.notify.queue.max-retries` | `3` | Maximum retry attempts for failed notifications |
| `fractal.notify.queue.retry-delay` | `60000` | Delay before retry in milliseconds |

---

## Database Schema

The library automatically creates the `notifications` table on startup (if `spring.jpa.hibernate.ddl-auto=update` is set).

### Schema

```sql
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    notification_type VARCHAR(20) NOT NULL,
    recipient_to TEXT[] NOT NULL,
    recipient_cc TEXT[],
    recipient_bcc TEXT[],
    subject VARCHAR(500),
    body TEXT,
    template_name VARCHAR(200),
    template_content TEXT,
    template_variables JSONB,
    from_address VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider VARCHAR(50),
    message_id VARCHAR(255),
    error_message TEXT,
    retry_count INT DEFAULT 0,
    cost DECIMAL(10,4),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_notification_status ON notifications(status);
CREATE INDEX idx_notification_created_at ON notifications(created_at);
CREATE INDEX idx_notification_type ON notifications(notification_type);
```

### Status Values

- **PENDING**: Notification persisted, waiting to be sent
- **PROCESSING**: Notification is currently being processed by scheduler (queue mode only)
- **SENT**: Successfully sent
- **FAILED**: Failed to send
- **RETRYING**: Currently retrying after failure (queue mode only)

### Persistence Flow

1. **Before Sending**: Notification is persisted with status `PENDING`
2. **After Sending**: Status updated to `SENT` or `FAILED`
3. **Crash Recovery**: PENDING notifications can be retried after system restart

---

## Extensibility

### Adding a New Provider

1. Implement the provider interface (e.g., `SMSProvider`)
2. Register in the provider factory
3. Add configuration properties

**Example:**
```java
@Component
public class AWSSNSProvider implements SMSProvider {
    @Override
    public SMSResponse sendSMS(SMSRequest request) {
        // AWS SNS implementation
    }
    
    @Override
    public String getProviderName() {
        return "aws-sns";
    }
}
```

### Adding a New Notification Type

1. Create strategy class implementing `NotificationStrategy`
2. Create provider interface
3. Implement provider(s)
4. Register in factories

### Adding Custom Async Publishers

You can add custom async publishers (e.g., RabbitMQ, Redis) by implementing `AsyncNotificationPublisher`. See `ADDING_ASYNC_PUBLISHER.md` for detailed guide.

---

## Key Benefits

- **Simple Integration**: Just add dependency and configure
- **Multiple Recipients**: Support for multiple TO, CC, BCC
- **Client Templates**: Pass template content directly or use library templates
- **Persistence**: Automatic database persistence with status tracking
- **Crash Recovery**: Notifications survive system crashes
- **Provider Flexibility**: Switch providers via configuration
- **Extensible**: Easy to add custom async publishers (RabbitMQ, Redis, etc.)
- **Clean API**: Builder pattern for easy usage

---

## Troubleshooting

### Notifications Not Persisting

- Check `fractal.notify.persistence.enabled=true`
- Verify database connection settings
- Check Spring Data JPA is configured

### Email Not Sending

- Verify SMTP credentials
- Check network connectivity
- Review application logs for errors

### Template Not Found

- For library templates: Ensure template exists in `resources/templates/{type}/{name}.html`
- For client templates: Use `templateContent()` instead of `template()`

---

## License

Copyright © 2024 Fractal Engineering Team

---

**Version**: 1.0.0  
**Last Updated**: 2024
