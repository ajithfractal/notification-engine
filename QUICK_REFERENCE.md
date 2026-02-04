# Notification Engine - Quick Reference Card

## 🚀 Setup (One Time)

```bash
# 1. Start Kafka
docker-compose up -d

# 2. Add dependency to pom.xml
<dependency>
    <groupId>com.fractal</groupId>
    <artifactId>fractal-notify</artifactId>
    <version>1.0.0</version>
</dependency>

# 3. Configure application.yml
spring:
  kafka:
    bootstrap-servers: localhost:9092

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

---

## 💻 Code Examples

### Email with Template
```java
@Autowired NotificationService notificationService;

NotificationRequest request = NotificationRequest.builder()
    .notificationType(NotificationType.EMAIL)
    .to("user@example.com")
    .subject("Welcome")
    .templateName("welcome")
    .templateVariables(Map.of("name", "John"))
    .build();

notificationService.sendAsync(request);
```

### Email Plain Text
```java
NotificationRequest request = NotificationRequest.builder()
    .notificationType(NotificationType.EMAIL)
    .to("user@example.com")
    .subject("Alert")
    .body("Your order is ready!")
    .build();

notificationService.sendAsync(request);
```

### SMS
```java
NotificationRequest request = NotificationRequest.builder()
    .notificationType(NotificationType.SMS)
    .to("+1234567890")
    .body("Your OTP: 123456")
    .build();

notificationService.sendAsync(request);
```

### Wait for Response
```java
NotificationResponse response = notificationService.send(request);
if (response.isSuccess()) {
    log.info("Sent: {}", response.getMessageId());
}
```

---

## ⚙️ Configuration

| Setting | Location | Example |
|---------|----------|---------|
| Kafka Server | `spring.kafka.bootstrap-servers` | `localhost:9092` |
| Email Host | `fractal.notify.email.smtp.host` | `smtp.gmail.com` |
| Email User | `fractal.notify.email.smtp.username` | `${EMAIL_USERNAME}` |
| Twilio SID | `fractal.notify.sms.twilio.account-sid` | `${TWILIO_ACCOUNT_SID}` |
| Twilio Token | `fractal.notify.sms.twilio.auth-token` | `${TWILIO_AUTH_TOKEN}` |

---

## 🔍 Troubleshooting

| Problem | Solution |
|---------|----------|
| Kafka connection error | Check `docker-compose ps` |
| Email not sending | Verify SMTP credentials |
| SMS not sending | Check Twilio credentials & phone format |
| Messages not processing | Check Kafka consumer logs |

---

## 📊 Monitoring

- **Kafka UI**: http://localhost:8080
- **Check Topics**: `docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092`
- **View Logs**: Check application logs for notification status

---

## 📝 Template Location

Templates: `src/main/resources/templates/email/{template-name}.html`

Use Thymeleaf: `<span th:text="${variable}">Default</span>`

---

## 🎯 Common Patterns

```java
// Welcome Email
sendEmail(user.getEmail(), "welcome", Map.of("name", user.getName()));

// OTP SMS
sendSMS(phoneNumber, "Your OTP: " + otp);

// Order Confirmation
sendEmail(order.getEmail(), "order-confirmation", Map.of("orderId", order.getId()));
```

---

**Full Guide**: See [TEAM_GUIDE.md](TEAM_GUIDE.md)
