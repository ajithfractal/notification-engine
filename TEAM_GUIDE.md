# Fractal Notification Engine - Team Guide

## 📋 What is This?

A **reusable notification engine** that lets you send **Email, SMS, and WhatsApp** notifications from any Spring Boot project. Just add it as a dependency and start sending notifications!

## ✨ Key Features

- ✅ **Multi-Channel**: Email, SMS, WhatsApp
- ✅ **Easy Provider Switching**: Change providers via config (no code changes)
- ✅ **Kafka-Based**: Reliable, scalable async processing
- ✅ **Template Support**: Use HTML templates for emails
- ✅ **Auto-Configuration**: Works out of the box

---

## 🚀 Quick Start

### Step 1: Add Dependency

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.fractal</groupId>
    <artifactId>fractal-notify</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Step 2: Start Kafka (Local Development)

```bash
docker-compose up -d
```

This starts:
- Kafka (port 9092)
- Kafka UI (http://localhost:8080)

### Step 3: Configure

Add to your `application.yml`:

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092

fractal:
  notify:
    enabled: true
    async:
      mode: kafka
    email:
      provider: smtp
      smtp:
        host: smtp.gmail.com
        port: 587
        username: ${EMAIL_USERNAME}
        password: ${EMAIL_PASSWORD}
        from: noreply@yourcompany.com
    sms:
      provider: twilio
      twilio:
        account-sid: ${TWILIO_ACCOUNT_SID}
        auth-token: ${TWILIO_AUTH_TOKEN}
        from-number: ${TWILIO_FROM_NUMBER}
```

### Step 4: Use It!

```java
@Autowired
private NotificationService notificationService;

public void sendWelcomeEmail(String email, String name) {
    NotificationRequest request = NotificationRequest.builder()
        .notificationType(NotificationType.EMAIL)
        .to(email)
        .subject("Welcome!")
        .templateName("welcome")
        .templateVariables(Map.of("name", name))
        .build();
    
    notificationService.sendAsync(request);
}
```

**That's it!** 🎉

---

## 📧 Sending Notifications

### Email with Template

```java
NotificationRequest request = NotificationRequest.builder()
    .notificationType(NotificationType.EMAIL)
    .to("user@example.com")
    .subject("Welcome to Our Platform")
    .templateName("welcome")  // Uses templates/email/welcome.html
    .templateVariables(Map.of(
        "name", "John Doe",
        "company", "Fractal"
    ))
    .build();

notificationService.sendAsync(request);
```

### Email without Template (Plain Text/HTML)

```java
NotificationRequest request = NotificationRequest.builder()
    .notificationType(NotificationType.EMAIL)
    .to("user@example.com")
    .subject("Order Confirmation")
    .body("<h1>Your order #12345 has been confirmed!</h1>")
    .build();

notificationService.sendAsync(request);
```

### SMS

```java
NotificationRequest request = NotificationRequest.builder()
    .notificationType(NotificationType.SMS)
    .to("+1234567890")
    .body("Your OTP is: 123456")
    .build();

notificationService.sendAsync(request);
```

### Synchronous (Wait for Response)

```java
NotificationResponse response = notificationService.send(request);

if (response.isSuccess()) {
    log.info("Sent! Message ID: {}", response.getMessageId());
} else {
    log.error("Failed: {}", response.getErrorMessage());
}
```

---

## ⚙️ Configuration Guide

### Email Configuration

**SMTP (Gmail, Outlook, etc.)**
```yaml
fractal:
  notify:
    email:
      provider: smtp
      smtp:
        host: smtp.gmail.com
        port: 587
        username: your-email@gmail.com
        password: your-app-password
        from: noreply@yourcompany.com
```

### SMS Configuration

**Twilio**
```yaml
fractal:
  notify:
    sms:
      provider: twilio
      twilio:
        account-sid: ${TWILIO_ACCOUNT_SID}
        auth-token: ${TWILIO_AUTH_TOKEN}
        from-number: ${TWILIO_FROM_NUMBER}  # Your Twilio phone number
```

### Kafka Configuration

```yaml
fractal:
  notify:
    async:
      mode: kafka  # Use Kafka for async processing
      kafka:
        topic: notifications
        consumer-group: notification-processors
        concurrency: 3  # Number of parallel consumers
```

### Switch to @Async (No Kafka)

```yaml
fractal:
  notify:
    async:
      mode: async  # Use Spring @Async instead
```

---

## 🔄 Switching Providers

### Example: Switch from Twilio to AWS SNS

**Just change the config:**

```yaml
fractal:
  notify:
    sms:
      provider: aws-sns  # Changed from 'twilio'
```

**No code changes needed!** The engine automatically uses the new provider.

---

## 📝 Creating Email Templates

1. Create template file: `src/main/resources/templates/email/my-template.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>My Template</title>
</head>
<body>
    <h1>Hello <span th:text="${name}">User</span>!</h1>
    <p>Welcome to <span th:text="${company}">Our Company</span>.</p>
</body>
</html>
```

2. Use it:

```java
NotificationRequest request = NotificationRequest.builder()
    .notificationType(NotificationType.EMAIL)
    .to("user@example.com")
    .subject("Welcome")
    .templateName("my-template")  // Without .html extension
    .templateVariables(Map.of(
        "name", "John",
        "company", "Fractal"
    ))
    .build();
```

---

## 🐛 Troubleshooting

### Kafka Connection Issues

**Problem**: `Connection refused` or `Bootstrap server not available`

**Solution**:
1. Check if Kafka is running: `docker-compose ps`
2. Verify bootstrap servers: `spring.kafka.bootstrap-servers: localhost:9092`
3. Check Kafka logs: `docker-compose logs kafka`

### Email Not Sending

**Problem**: Emails not being delivered

**Solution**:
1. Check SMTP credentials
2. For Gmail, use **App Password** (not regular password)
3. Check firewall/network settings
4. Verify `from` address is valid

### SMS Not Sending

**Problem**: SMS not being delivered via Twilio

**Solution**:
1. Verify Twilio credentials (Account SID, Auth Token)
2. Check `from-number` is a valid Twilio phone number
3. Verify phone number format: `+1234567890` (with country code)
4. Check Twilio account balance

### Messages Not Processing

**Problem**: Notifications published but not processed

**Solution**:
1. Check Kafka consumer logs
2. Verify consumer group is active: Check Kafka UI
3. Check if consumer is running (should see logs)
4. Verify topic exists: `docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092`

---

## 📊 Monitoring

### Kafka UI

Access at: **http://localhost:8080**

- View topics and messages
- Monitor consumer groups
- Check message lag
- Browse message content

### Application Logs

The engine logs:
- When notifications are published to Kafka
- When notifications are processed
- Success/failure status
- Provider used

Example logs:
```
INFO  - Publishing notification request to Kafka topic: notifications
INFO  - Received notification request from Kafka - Type: EMAIL, To: user@example.com
INFO  - Notification sent successfully. Provider: smtp, MessageId: smtp-1234567890
```

---

## 🔧 Advanced Usage

### Handle Response

```java
CompletableFuture<NotificationResponse> future = 
    notificationService.sendAsync(request);

future.thenAccept(response -> {
    if (response.isSuccess()) {
        log.info("Notification sent: {}", response.getMessageId());
        // Update database, send webhook, etc.
    } else {
        log.error("Failed: {}", response.getErrorMessage());
        // Handle error
    }
});
```

### Custom From Address

```java
NotificationRequest request = NotificationRequest.builder()
    .notificationType(NotificationType.EMAIL)
    .to("user@example.com")
    .from("custom@yourcompany.com")  // Override default
    .subject("Custom Email")
    .body("Content")
    .build();
```

---

## 📚 Common Use Cases

### 1. Welcome Email

```java
public void sendWelcomeEmail(User user) {
    NotificationRequest request = NotificationRequest.builder()
        .notificationType(NotificationType.EMAIL)
        .to(user.getEmail())
        .subject("Welcome to Our Platform!")
        .templateName("welcome")
        .templateVariables(Map.of(
            "name", user.getName(),
            "company", "Fractal"
        ))
        .build();
    
    notificationService.sendAsync(request);
}
```

### 2. OTP via SMS

```java
public void sendOTP(String phoneNumber, String otp) {
    NotificationRequest request = NotificationRequest.builder()
        .notificationType(NotificationType.SMS)
        .to(phoneNumber)
        .body("Your verification code is: " + otp + ". Valid for 5 minutes.")
        .build();
    
    notificationService.sendAsync(request);
}
```

### 3. Order Confirmation

```java
public void sendOrderConfirmation(Order order) {
    NotificationRequest request = NotificationRequest.builder()
        .notificationType(NotificationType.EMAIL)
        .to(order.getCustomerEmail())
        .subject("Order #" + order.getId() + " Confirmed")
        .templateName("order-confirmation")
        .templateVariables(Map.of(
            "orderId", order.getId(),
            "total", order.getTotal(),
            "items", order.getItems()
        ))
        .build();
    
    notificationService.sendAsync(request);
}
```

---

## 🎯 Best Practices

1. **Always use async** (`sendAsync`) unless you need to wait for the result
2. **Use templates** for emails to maintain consistency
3. **Store credentials** in environment variables, not in code
4. **Monitor Kafka** using Kafka UI for production
5. **Handle errors** gracefully - check `response.isSuccess()`
6. **Use appropriate notification types** - Email for long content, SMS for short alerts

---

## 🆘 Need Help?

1. Check logs first
2. Verify configuration
3. Test Kafka connectivity
4. Check provider credentials
5. Review Kafka UI for message flow

---

## 📖 Architecture Overview

```
Your Application
    ↓
NotificationService
    ↓
Kafka Topic (notifications)
    ↓
Kafka Consumer
    ↓
Email/SMS/WhatsApp Provider
    ↓
External Service (SMTP, Twilio, etc.)
```

**Benefits:**
- ✅ Decoupled: Your API doesn't wait for notification delivery
- ✅ Reliable: Messages persist in Kafka
- ✅ Scalable: Multiple consumers can process in parallel
- ✅ Fault Tolerant: Messages survive app restarts

---

## 🚦 Quick Reference

### Notification Types
- `NotificationType.EMAIL`
- `NotificationType.SMS`
- `NotificationType.WHATSAPP`

### Methods
- `sendAsync(request)` - Send asynchronously (recommended)
- `send(request)` - Send synchronously (waits for result)

### Response
- `response.isSuccess()` - Check if successful
- `response.getMessageId()` - Get message ID
- `response.getErrorMessage()` - Get error if failed

---

**Happy Notifying! 🎉**

For detailed technical documentation, see [README.md](README.md) and [KAFKA_SETUP.md](KAFKA_SETUP.md)
