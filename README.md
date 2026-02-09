# Fractal Notification Engine

A powerful, production-ready Spring Boot notification library for sending multi-channel notifications (Email, SMS, WhatsApp) with PostgreSQL persistence, RabbitMQ support, template management, static asset handling, and Azure Blob Storage integration.

---

## Table of Contents

1. [Features](#features)
2. [Quick Start](#quick-start)
3. [Configuration](#configuration)
4. [Usage Examples](#usage-examples)
5. [RabbitMQ Integration](#rabbitmq-integration)
6. [Static Assets](#static-assets)
7. [Email Attachments](#email-attachments)
8. [Templates](#templates)
9. [Architecture](#architecture)
10. [Database Schema](#database-schema)
11. [Extensibility](#extensibility)
12. [Troubleshooting](#troubleshooting)

---

## Features

### Core Features

- **Multi-Channel Support**: Email, SMS, WhatsApp (extensible)
- **Multiple Recipients**: Support for multiple TO, CC, and BCC recipients
- **Template Engine**: Thymeleaf-based templates with variable substitution
- **PostgreSQL Persistence**: Automatic persistence with status tracking (PENDING, PROCESSING, SENT, FAILED)
- **Crash Recovery**: Notifications persist before sending, survive system crashes
- **Provider Agnostic**: Switch providers via configuration (e.g., Twilio → AWS SNS)
- **Simple API**: Clean `NotificationUtils` builder pattern for easy integration

### Advanced Features

- **RabbitMQ Integration**: Publish notifications to RabbitMQ for distributed processing
- **Database Queue**: Optional database-based queue with scheduler for processing
- **Async Processing**: Java built-in async (@Async) - simple and efficient
- **Email Attachments**: Support for File, byte[], InputStream, and MultipartFile attachments
- **Static Assets**: Reusable logos, headers, footers stored in database and referenced via URLs (not attachments)
- **Azure Blob Storage**: Store attachments and static assets in Azure Blob Storage
- **Template Management**: Library templates or client-provided templates
- **Status Tracking**: Complete notification lifecycle tracking

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
# Mode options: "async" (default), "rabbitmq", or "queue"
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
fractal.notify.email.smtp.reply-to=${EMAIL_REPLY_TO:}

# SMS Configuration
fractal.notify.sms.provider=twilio
fractal.notify.sms.twilio.account-sid=${TWILIO_ACCOUNT_SID}
fractal.notify.sms.twilio.auth-token=${TWILIO_AUTH_TOKEN}
fractal.notify.sms.twilio.from-number=${TWILIO_FROM_NUMBER}

# WhatsApp Configuration
fractal.notify.whatsapp.provider=twilio
fractal.notify.whatsapp.enabled=true
fractal.notify.whatsapp.twilio.account-sid=${TWILIO_WHATSAPP_ACCOUNT_SID}
fractal.notify.whatsapp.twilio.auth-token=${TWILIO_WHATSAPP_AUTH_TOKEN}
fractal.notify.whatsapp.twilio.whatsapp-from-number=${TWILIO_WHATSAPP_FROM_NUMBER}

# Persistence Configuration (PostgreSQL)
fractal.notify.persistence.enabled=true
fractal.notify.persistence.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/notifications}
fractal.notify.persistence.datasource.username=${DB_USERNAME:postgres}
fractal.notify.persistence.datasource.password=${DB_PASSWORD:postgres}
fractal.notify.persistence.datasource.driver-class-name=org.postgresql.Driver

# Storage Configuration (for email attachments and static assets)
fractal.notify.storage.provider=azure-blob
fractal.notify.storage.azure-blob.connection-string=${AZURE_STORAGE_CONNECTION_STRING}
fractal.notify.storage.azure-blob.container-name=${AZURE_STORAGE_CONTAINER_NAME:email-attachments}

# Spring Data JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

### 3. Enable Component Scanning

```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.fractal.notify", "your.package"})
@EnableJpaRepositories(basePackages = {
    "com.fractal.notify.persistence.repository",
    "com.fractal.notify.template.repository",
    "com.fractal.notify.staticasset.repository"
})
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
# Mode options: "async" (default), "rabbitmq", or "queue"
fractal.notify.async.mode=async
fractal.notify.async.enabled=true
fractal.notify.async.core-pool-size=5
fractal.notify.async.max-pool-size=10
fractal.notify.async.queue-capacity=100

# RabbitMQ Configuration (when async.mode=rabbitmq)
fractal.notify.rabbitmq.host=localhost
fractal.notify.rabbitmq.port=5672
fractal.notify.rabbitmq.username=guest
fractal.notify.rabbitmq.password=guest
fractal.notify.rabbitmq.virtual-host=/
fractal.notify.rabbitmq.exchange=fractal.notifications
fractal.notify.rabbitmq.queue.email=fractal.notifications.email
fractal.notify.rabbitmq.queue.sms=fractal.notifications.sms
fractal.notify.rabbitmq.queue.whatsapp=fractal.notifications.whatsapp
fractal.notify.rabbitmq.routing-key.email=notification.email
fractal.notify.rabbitmq.routing-key.sms=notification.sms
fractal.notify.rabbitmq.routing-key.whatsapp=notification.whatsapp
fractal.notify.rabbitmq.connection-timeout=60000
fractal.notify.rabbitmq.requested-heartbeat=60

# Email
fractal.notify.email.provider=smtp
fractal.notify.email.smtp.host=smtp.gmail.com
fractal.notify.email.smtp.port=587
fractal.notify.email.smtp.username=${EMAIL_USERNAME}
fractal.notify.email.smtp.password=${EMAIL_PASSWORD}
fractal.notify.email.smtp.from=${EMAIL_FROM}
fractal.notify.email.smtp.reply-to=${EMAIL_REPLY_TO}

# SMS
fractal.notify.sms.provider=twilio
fractal.notify.sms.twilio.account-sid=${TWILIO_ACCOUNT_SID}
fractal.notify.sms.twilio.auth-token=${TWILIO_AUTH_TOKEN}
fractal.notify.sms.twilio.from-number=${TWILIO_FROM_NUMBER}

# WhatsApp
fractal.notify.whatsapp.provider=twilio
fractal.notify.whatsapp.enabled=true
fractal.notify.whatsapp.twilio.account-sid=${TWILIO_WHATSAPP_ACCOUNT_SID}
fractal.notify.whatsapp.twilio.auth-token=${TWILIO_WHATSAPP_AUTH_TOKEN}
fractal.notify.whatsapp.twilio.whatsapp-from-number=${TWILIO_WHATSAPP_FROM_NUMBER}

# Persistence
fractal.notify.persistence.enabled=true
fractal.notify.persistence.datasource.url=jdbc:postgresql://localhost:5432/notifications
fractal.notify.persistence.datasource.username=${DB_USERNAME}
fractal.notify.persistence.datasource.password=${DB_PASSWORD}
fractal.notify.persistence.datasource.driver-class-name=org.postgresql.Driver

# Queue Configuration (Database Queue)
fractal.notify.queue.enabled=false
fractal.notify.queue.poll-interval=5000
fractal.notify.queue.batch-size=10
fractal.notify.queue.max-retries=3
fractal.notify.queue.retry-delay=60000

# Storage
fractal.notify.storage.provider=azure-blob
fractal.notify.storage.azure-blob.connection-string=${AZURE_STORAGE_CONNECTION_STRING}
fractal.notify.storage.azure-blob.container-name=${AZURE_STORAGE_CONTAINER_NAME}
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
    .template("welcome")  // Loads from database or resources/templates/email/welcome.html
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

#### Email with Attachments
```java
// File attachment
notificationUtils.email()
    .to("user@example.com")
    .subject("Invoice")
    .body("Please find your invoice attached.")
    .attachment(new File("invoice.pdf"))
    .send();

// Byte array attachment
byte[] pdfBytes = generatePdf();
notificationUtils.email()
    .to("user@example.com")
    .subject("Report")
    .body("Please find the report attached.")
    .attachment(pdfBytes, "report.pdf")
    .send();

// InputStream attachment
InputStream reportStream = getReportStream();
notificationUtils.email()
    .to("user@example.com")
    .subject("Monthly Report")
    .body("Please find the monthly report attached.")
    .attachment(reportStream, "monthly-report.pdf", "application/pdf")
    .send();

// MultipartFile attachment (from Spring REST controller)
@PostMapping("/send-email")
public void sendEmail(@RequestParam("file") MultipartFile file) {
    notificationUtils.email()
        .to("user@example.com")
        .subject("Document")
        .body("Please find the document attached.")
        .attachment(file)
        .send();
}
```

#### Email with Static Assets (Logo, Header, Footer)
```java
// HTML body with cid: references
String htmlBody = """
    <html>
    <body style='font-family: Arial, sans-serif;'>
        <img src='cid:header' alt='Header' style='width: 100%;' />
        <div style='text-align: center; padding: 20px;'>
            <img src='cid:logo' alt='Company Logo' style='max-width: 200px;' />
        </div>
        <div style='padding: 20px;'>
            <h1>Welcome, {{name}}!</h1>
            <p>Thank you for joining us.</p>
        </div>
        <img src='cid:footer' alt='Footer' style='width: 100%;' />
    </body>
    </html>
    """;

notificationUtils.email()
    .to("user@example.com")
    .subject("Welcome")
    .body(htmlBody)  // cid: references automatically replaced with URLs
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

// Multiple recipients
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

## RabbitMQ Integration

The library can act as a **producer** that publishes notifications to RabbitMQ. A separate consumer application processes these messages and sends the actual notifications.

### Producer Configuration

Set `fractal.notify.async.mode=rabbitmq`:

```properties
# Enable RabbitMQ mode
fractal.notify.async.mode=rabbitmq

# RabbitMQ Connection
fractal.notify.rabbitmq.host=localhost
fractal.notify.rabbitmq.port=5672
fractal.notify.rabbitmq.username=guest
fractal.notify.rabbitmq.password=guest
fractal.notify.rabbitmq.virtual-host=/

# Exchange and Queues
fractal.notify.rabbitmq.exchange=fractal.notifications
fractal.notify.rabbitmq.queue.email=fractal.notifications.email
fractal.notify.rabbitmq.queue.sms=fractal.notifications.sms
fractal.notify.rabbitmq.queue.whatsapp=fractal.notifications.whatsapp

# Routing Keys
fractal.notify.rabbitmq.routing-key.email=notification.email
fractal.notify.rabbitmq.routing-key.sms=notification.sms
fractal.notify.rabbitmq.routing-key.whatsapp=notification.whatsapp
```

### How It Works

1. **Producer (This Library)**:
   - Persists notification to database with `PENDING` status
   - Publishes `NotificationMessage` to RabbitMQ exchange
   - Returns immediately with success response

2. **Consumer (Separate Application)**:
   - Consumes messages from RabbitMQ queues
   - Updates notification status to `PROCESSING`
   - Sends notification via provider (Email/SMS/WhatsApp)
   - Updates notification status to `SENT` or `FAILED`

### Message Flow

```
Producer Application
    ↓
NotificationService.sendAsync()
    ↓
Persist to DB (status: PENDING)
    ↓
Publish to RabbitMQ Exchange
    ↓
RabbitMQ Queues (email/sms/whatsapp)
    ↓
Consumer Application
    ↓
Update DB (status: PROCESSING)
    ↓
Send Notification
    ↓
Update DB (status: SENT/FAILED)
```

### Creating a Consumer Application

See the separate consumer application example (`fractal-notify-consumer`) that:
- Consumes messages from RabbitMQ
- Uses this library as a dependency for sending notifications
- Updates notification status in the shared database

---

## Static Assets

Static assets (logos, headers, footers) are stored in the database and automatically referenced in email templates via URLs. They are **NOT attachments** - they're embedded inline images referenced directly in HTML.

### How It Works

1. **Store static assets** in the `static_assets` table
2. **Reference them in HTML** using `cid:` syntax: `<img src='cid:logo'>`
3. **System automatically replaces** `cid:` references with direct URLs to storage
4. **No attachments** - images load from URLs, not attached files

### Database Setup

```sql
INSERT INTO static_assets (name, storage_path, file_name, content_type, content_id, is_active, description)
VALUES 
    ('company-logo', 'static-assets/logo.png', 'logo.png', 'image/png', 'logo', true, 'Company logo'),
    ('email-header', 'static-assets/header.png', 'header.png', 'image/png', 'header', true, 'Email header'),
    ('email-footer', 'static-assets/footer.png', 'footer.png', 'image/png', 'footer', true, 'Email footer');
```

### Usage in Email Templates

```html
<html>
<body>
    <img src='cid:header' alt='Header' />
    <div style='text-align: center;'>
        <img src='cid:logo' alt='Company Logo' />
    </div>
    <h1>Welcome!</h1>
    <p>Thank you for joining us.</p>
    <img src='cid:footer' alt='Footer' />
</body>
</html>
```

### Benefits

- ✅ **No attachments** - Images load from URLs
- ✅ **No duplicate uploads** - Assets stored once, referenced many times
- ✅ **Easy updates** - Change logo in DB, all emails use new version
- ✅ **Faster emails** - Smaller message size
- ✅ **Better caching** - Browsers cache images

For detailed information, see [STATIC_ASSETS_GUIDE.md](STATIC_ASSETS_GUIDE.md).

---

## Email Attachments

The library supports multiple attachment types:

### Supported Attachment Types

1. **File**: `attachment(new File("document.pdf"))`
2. **Byte Array**: `attachment(bytes, "document.pdf")`
3. **InputStream**: `attachment(inputStream, "document.pdf", "application/pdf")`
4. **MultipartFile**: `attachment(multipartFile)` (from Spring file uploads)
5. **Storage Path Reference**: Reference existing files in storage without re-uploading

### Attachment Storage

Attachments are automatically uploaded to Azure Blob Storage (or configured storage provider) and metadata is stored in the database. When using RabbitMQ, attachment storage paths are included in the message.

### Example: Multiple Attachments

```java
notificationUtils.email()
    .to("user@example.com")
    .subject("Documents")
    .body("Please find the documents attached.")
    .attachment(new File("invoice.pdf"))
    .attachment(reportBytes, "report.pdf")
    .attachment(dataStream, "data.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    .send();
```

---

## Templates

### Library Templates

Templates can be stored in the database (`templates` table) or in `resources/templates/{type}/{name}.html`:

```java
notificationUtils.email()
    .to("user@example.com")
    .subject("Welcome")
    .template("welcome")  // Loads from database or resources/templates/email/welcome.html
    .variable("name", "John")
    .variable("resetLink", "https://example.com/reset?token=abc123")
    .send();
```

### Client-Provided Templates

Pass template content directly:

```java
String template = """
    <html>
        <body>
            <h1>Hello, <span th:text="${name}">User</span>!</h1>
            <p>Click <a th:href="${resetLink}">here</a> to reset your password.</p>
        </body>
    </html>
    """;

notificationUtils.email()
    .to("user@example.com")
    .subject("Password Reset")
    .templateContent(template)
    .variable("name", "John")
    .variable("resetLink", "https://example.com/reset?token=abc123")
    .send();
```

### Template Variables

Use Thymeleaf syntax (`${variable}`) or simple placeholders (`{{variable}}`) in templates. Variables are passed via `.variable(key, value)`.

---

## Architecture

### Current Flow (Async Mode)

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

### RabbitMQ Flow

```
Client Application
    ↓
NotificationUtils (Builder API)
    ↓
NotificationService
    ↓
NotificationPersistenceService (Persist to DB with PENDING status)
    ↓
RabbitMQNotificationPublisher
    ↓
RabbitMQ Exchange → Queues
    ↓
Consumer Application (Separate)
    ↓
NotificationService.send() (from library)
    ↓
NotificationStrategy → Provider → External Service
    ↓
NotificationPersistenceService (Update status to PROCESSING → SENT/FAILED)
```

### Key Components

1. **NotificationUtils**: Simple builder API for clients
2. **NotificationService**: Core processing and routing
3. **NotificationPersistenceService**: Database persistence and status tracking
4. **AsyncNotificationPublisher**: Abstraction for async processing (@Async or RabbitMQ)
5. **RabbitMQNotificationPublisher**: Publishes messages to RabbitMQ
6. **NotificationStrategy**: Handles different notification types (Email, SMS, WhatsApp)
7. **Provider**: Actual implementation (SMTP, Twilio, etc.)
8. **TemplateService**: Renders templates (library or client-provided)
9. **StaticAssetService**: Replaces `cid:` references with URLs
10. **StorageProvider**: Handles file uploads/downloads (Azure Blob Storage)

### Design Patterns

- **Strategy Pattern**: Different notification types (Email, SMS, WhatsApp)
- **Provider Pattern**: Multiple implementations per type (Twilio, AWS SNS, etc.)
- **Factory Pattern**: Creates providers based on configuration
- **Builder Pattern**: Fluent API in NotificationUtils

---

## Database Schema

The library automatically creates tables on startup (if `spring.jpa.hibernate.ddl-auto=update` is set) or via Flyway migrations.

### Notifications Table

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

### Email Attachments Table

```sql
CREATE TABLE email_attachments (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    file_name VARCHAR(500) NOT NULL,
    content_type VARCHAR(255),
    file_size BIGINT,
    storage_provider VARCHAR(50) NOT NULL,
    storage_path TEXT NOT NULL,
    is_inline BOOLEAN DEFAULT false,
    content_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_attachment_notification_id ON email_attachments(notification_id);
```

### Static Assets Table

```sql
CREATE TABLE static_assets (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    storage_path TEXT NOT NULL,
    public_url TEXT,
    file_name VARCHAR(500) NOT NULL,
    content_type VARCHAR(255),
    file_size BIGINT,
    storage_provider VARCHAR(50) NOT NULL DEFAULT 'azure-blob',
    content_id VARCHAR(255) NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_static_asset_name ON static_assets(name);
CREATE INDEX idx_static_asset_active ON static_assets(is_active);
CREATE INDEX idx_static_asset_content_id ON static_assets(content_id) WHERE content_id IS NOT NULL;
```

### Templates Table

```sql
CREATE TABLE templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    notification_type VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_template_name ON templates(name);
CREATE INDEX idx_template_type ON templates(notification_type);
CREATE INDEX idx_template_active ON templates(is_active);
```

### Status Values

- **PENDING**: Notification persisted, waiting to be sent (or published to RabbitMQ)
- **PROCESSING**: Currently being processed by consumer/scheduler
- **SENT**: Successfully sent
- **FAILED**: Failed to send
- **RETRYING**: Currently retrying after failure

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

---

## Troubleshooting

### Notifications Not Persisting

- Check `fractal.notify.persistence.enabled=true`
- Verify database connection settings
- Check Spring Data JPA is configured
- Ensure component scanning includes `com.fractal.notify`

### Email Not Sending

- Verify SMTP credentials
- Check network connectivity
- Review application logs for errors
- Ensure `fractal.notify.email.smtp.from` is set

### RabbitMQ Connection Issues

- Verify RabbitMQ is running
- Check connection credentials (`username`, `password`)
- Verify virtual host exists
- Check network connectivity to RabbitMQ server
- Review RabbitMQ logs

### Template Not Found

- For library templates: Ensure template exists in database or `resources/templates/{type}/{name}.html`
- For client templates: Use `templateContent()` instead of `template()`
- Check template name matches exactly (case-sensitive)

### Static Assets Not Showing

- Verify static asset exists in `static_assets` table
- Check `content_id` matches `cid:` reference in HTML
- Ensure `is_active=true` for the asset
- Verify storage path is correct and file exists in storage
- Check public URL generation (SAS URL for Azure Blob Storage)

### Attachments Not Working

- Verify storage provider is configured correctly
- Check Azure Blob Storage connection string
- Ensure container exists and is accessible
- Review storage logs for upload errors
- Verify file size limits

---

## License

Copyright © 2024 Fractal Engineering Team

---

**Version**: 1.0.0  
**Last Updated**: 2024
