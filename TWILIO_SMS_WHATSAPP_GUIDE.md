# Twilio SMS & WhatsApp Integration Guide

Complete guide for using Twilio SMS and WhatsApp messaging in your application with Fractal Notify.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Setup & Configuration](#setup--configuration)
3. [SMS Notifications](#sms-notifications)
4. [WhatsApp Notifications](#whatsapp-notifications)
5. [Error Handling](#error-handling)
6. [Best Practices](#best-practices)
7. [Troubleshooting](#troubleshooting)

---

## Prerequisites

1. **Twilio Account**: Sign up at [twilio.com](https://www.twilio.com)
2. **Twilio Credentials**:
   - Account SID
   - Auth Token
   - Phone Number (for SMS)
   - WhatsApp-enabled number (for WhatsApp)
3. **Maven Dependency**: Already included in `fractal-notify`

---

## Setup & Configuration

### Step 1: Add Dependency to Your Application

**Maven (`pom.xml`):**
```xml
<dependency>
    <groupId>com.fractal</groupId>
    <artifactId>fractal-notify</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Gradle (`build.gradle`):**
```gradle
dependencies {
    implementation 'com.fractal:fractal-notify:1.0.0'
}
```

### Step 2: Configure Twilio Credentials

Add to your `application.properties` or `application.yml`:

**application.properties:**
```properties
# Enable notification engine
fractal.notify.enabled=true

# SMS Configuration - Twilio
fractal.notify.sms.provider=twilio
fractal.notify.sms.twilio.account-sid=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
fractal.notify.sms.twilio.auth-token=your_auth_token_here
fractal.notify.sms.twilio.from-number=+1234567890

# WhatsApp Configuration
fractal.notify.whatsapp.provider=default
fractal.notify.whatsapp.enabled=true

# Persistence (Optional but recommended)
fractal.notify.persistence.enabled=true
fractal.notify.persistence.datasource.url=jdbc:postgresql://localhost:5432/notifications
fractal.notify.persistence.datasource.username=postgres
fractal.notify.persistence.datasource.password=postgres
fractal.notify.persistence.datasource.driver-class-name=org.postgresql.Driver
```

**Or use Environment Variables:**
```properties
fractal.notify.sms.twilio.account-sid=${TWILIO_ACCOUNT_SID}
fractal.notify.sms.twilio.auth-token=${TWILIO_AUTH_TOKEN}
fractal.notify.sms.twilio.from-number=${TWILIO_FROM_NUMBER}
```

### Step 3: Enable Component Scanning

Make sure your Spring Boot application scans the Fractal Notify package:

```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.fractal.notify", "com.yourpackage"})
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

---

## SMS Notifications

### Basic SMS - Single Recipient

```java
import com.fractal.notify.NotificationUtils;
import com.fractal.notify.core.NotificationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    
    @Autowired
    private NotificationUtils notificationUtils;
    
    public void sendSMS() {
        // Simple SMS
        notificationUtils.sms()
            .to("+1234567890")
            .body("Your OTP is: 123456")
            .send();
    }
}
```

### SMS with Custom From Number

```java
notificationUtils.sms()
    .to("+1234567890")
    .from("+1987654321")  // Override default from number
    .body("Hello from custom number!")
    .send();
```

### SMS - Multiple Recipients

```java
// Send to multiple recipients
notificationUtils.sms()
    .to("+1234567890", "+0987654321", "+1122334455")
    .body("Important alert: System maintenance scheduled")
    .send();
```

### SMS - Using List

```java
List<String> recipients = Arrays.asList(
    "+1234567890",
    "+0987654321"
);

notificationUtils.sms()
    .to(recipients)
    .body("Bulk SMS notification")
    .send();
```

### SMS - Synchronous (Wait for Response)

```java
NotificationResponse response = notificationUtils.sms()
    .to("+1234567890")
    .body("Your verification code is: 456789")
    .sendSync();

if (response.isSuccess()) {
    System.out.println("SMS sent! Message ID: " + response.getMessageId());
    // Message ID is the Twilio Message SID (e.g., SMxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx)
} else {
    System.out.println("SMS failed: " + response.getErrorMessage());
}
```

### SMS - Asynchronous (Fire and Forget)

```java
CompletableFuture<NotificationResponse> future = notificationUtils.sms()
    .to("+1234567890")
    .body("Your order has been shipped")
    .send();

// Handle response later
future.thenAccept(response -> {
    if (response.isSuccess()) {
        log.info("SMS sent successfully: {}", response.getMessageId());
    } else {
        log.error("SMS failed: {}", response.getErrorMessage());
    }
});
```

### SMS - Using Template (from Database)

```java
// First, add SMS template to database
// INSERT INTO templates (name, notification_type, content, is_active)
// VALUES ('otp-sms', 'SMS', 'Your OTP is: ${otp}. Valid for 5 minutes.', true);

notificationUtils.sms()
    .to("+1234567890")
    .template("otp-sms")
    .variable("otp", "123456")
    .send();
```

---

## WhatsApp Notifications

### Basic WhatsApp Message

```java
notificationUtils.whatsapp()
    .to("+1234567890")  // WhatsApp number (must include country code)
    .body("Hello! This is a WhatsApp message from your application.")
    .send();
```

### WhatsApp - Synchronous

```java
NotificationResponse response = notificationUtils.whatsapp()
    .to("+1234567890")
    .body("Your order #12345 has been confirmed!")
    .sendSync();

if (response.isSuccess()) {
    System.out.println("WhatsApp sent! Message ID: " + response.getMessageId());
} else {
    System.out.println("WhatsApp failed: " + response.getErrorMessage());
}
```

### WhatsApp - Asynchronous

```java
CompletableFuture<NotificationResponse> future = notificationUtils.whatsapp()
    .to("+1234567890")
    .body("Your appointment is scheduled for tomorrow at 2 PM")
    .send();

future.thenAccept(response -> {
    if (response.isSuccess()) {
        log.info("WhatsApp sent: {}", response.getMessageId());
    }
});
```

### WhatsApp - Using Template

```java
// Add WhatsApp template to database
// INSERT INTO templates (name, notification_type, content, is_active)
// VALUES ('order-confirmation-wa', 'WHATSAPP', 
//         'Hi ${customerName}! Your order #${orderId} for $${amount} has been confirmed. Thank you!', 
//         true);

notificationUtils.whatsapp()
    .to("+1234567890")
    .template("order-confirmation-wa")
    .variable("customerName", "John Doe")
    .variable("orderId", "ORD-12345")
    .variable("amount", "99.99")
    .send();
```

---

## Complete Example: Service Class

```java
package com.yourpackage.service;

import com.fractal.notify.NotificationUtils;
import com.fractal.notify.core.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationService {
    
    private final NotificationUtils notificationUtils;
    
    /**
     * Send OTP via SMS
     */
    public void sendOTP(String phoneNumber, String otp) {
        notificationUtils.sms()
            .to(phoneNumber)
            .body("Your verification code is: " + otp + ". Valid for 5 minutes.")
            .send()
            .thenAccept(response -> {
                if (response.isSuccess()) {
                    log.info("OTP sent to {}: {}", phoneNumber, response.getMessageId());
                } else {
                    log.error("Failed to send OTP to {}: {}", phoneNumber, response.getErrorMessage());
                }
            });
    }
    
    /**
     * Send order confirmation via WhatsApp
     */
    public CompletableFuture<NotificationResponse> sendOrderConfirmation(
            String whatsappNumber, 
            String orderId, 
            String customerName) {
        
        return notificationUtils.whatsapp()
            .to(whatsappNumber)
            .template("order-confirmation-wa")
            .variable("orderId", orderId)
            .variable("customerName", customerName)
            .variable("amount", "99.99")
            .send();
    }
    
    /**
     * Send bulk SMS notification
     */
    public void sendBulkSMS(List<String> phoneNumbers, String message) {
        notificationUtils.sms()
            .to(phoneNumbers)
            .body(message)
            .send()
            .thenAccept(response -> {
                if (response.isSuccess()) {
                    log.info("Bulk SMS sent: {}", response.getMessageId());
                }
            });
    }
    
    /**
     * Send SMS with error handling
     */
    public boolean sendSMSWithRetry(String phoneNumber, String message) {
        try {
            NotificationResponse response = notificationUtils.sms()
                .to(phoneNumber)
                .body(message)
                .sendSync();
            
            return response.isSuccess();
        } catch (Exception e) {
            log.error("Error sending SMS to {}", phoneNumber, e);
            return false;
        }
    }
}
```

---

## Error Handling

### Check Response Status

```java
CompletableFuture<NotificationResponse> future = notificationUtils.sms()
    .to("+1234567890")
    .body("Test message")
    .send();

future.thenAccept(response -> {
    if (response.isSuccess()) {
        // Success
        String messageId = response.getMessageId();  // Twilio Message SID
        String provider = response.getProvider();      // "twilio"
        log.info("SMS sent successfully. Message ID: {}", messageId);
    } else {
        // Failure
        String error = response.getErrorMessage();
        log.error("SMS failed: {}", error);
    }
});
```

### Handle Exceptions

```java
try {
    NotificationResponse response = notificationUtils.sms()
        .to("+1234567890")
        .body("Test")
        .sendSync();
    
    if (!response.isSuccess()) {
        // Handle failure
        log.error("SMS failed: {}", response.getErrorMessage());
    }
} catch (Exception e) {
    // Handle exception (e.g., template not found, invalid configuration)
    log.error("Error sending SMS", e);
}
```

### Common Error Scenarios

| Error | Cause | Solution |
|-------|-------|----------|
| `Template not found` | Template doesn't exist in database | Add template to `templates` table |
| `Twilio not configured` | Missing credentials | Check `application.properties` |
| `Invalid phone number` | Wrong format | Use E.164 format: `+1234567890` |
| `Insufficient balance` | Twilio account has no credits | Add credits to Twilio account |

---

## Best Practices

### 1. Phone Number Format

Always use E.164 format (international format with country code):

```java
// ✅ Correct
.to("+1234567890")      // US number
.to("+919876543210")    // India number

// ❌ Wrong
.to("1234567890")       // Missing country code
.to("(123) 456-7890")  // Wrong format
```

### 2. Use Environment Variables for Credentials

Never hardcode credentials:

```properties
# ✅ Good - Use environment variables
fractal.notify.sms.twilio.account-sid=${TWILIO_ACCOUNT_SID}
fractal.notify.sms.twilio.auth-token=${TWILIO_AUTH_TOKEN}

# ❌ Bad - Hardcoded
fractal.notify.sms.twilio.account-sid=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### 3. Handle Async Responses

```java
// ✅ Good - Handle response
notificationUtils.sms()
    .to(phoneNumber)
    .body(message)
    .send()
    .thenAccept(response -> {
        if (response.isSuccess()) {
            // Log success
        } else {
            // Handle error
        }
    })
    .exceptionally(throwable -> {
        log.error("Exception sending SMS", throwable);
        return null;
    });

// ❌ Bad - Fire and forget without handling
notificationUtils.sms().to(phoneNumber).body(message).send();
```

### 4. Use Templates for Dynamic Content

```java
// ✅ Good - Use database template
notificationUtils.sms()
    .to(phoneNumber)
    .template("otp-sms")
    .variable("otp", otpCode)
    .send();

// ❌ Less ideal - Hardcoded message
notificationUtils.sms()
    .to(phoneNumber)
    .body("Your OTP is: " + otpCode)  // Hard to maintain
    .send();
```

### 5. Validate Phone Numbers

```java
public boolean isValidPhoneNumber(String phoneNumber) {
    // E.164 format: + followed by 1-15 digits
    return phoneNumber != null 
        && phoneNumber.matches("^\\+[1-9]\\d{1,14}$");
}

// Use before sending
if (isValidPhoneNumber(phoneNumber)) {
    notificationUtils.sms().to(phoneNumber).body(message).send();
} else {
    log.error("Invalid phone number: {}", phoneNumber);
}
```

---

## Configuration Reference

### SMS Configuration Properties

| Property | Description | Required | Default |
|----------|-------------|----------|---------|
| `fractal.notify.sms.provider` | Provider name (currently only "twilio") | Yes | `twilio` |
| `fractal.notify.sms.twilio.account-sid` | Twilio Account SID | Yes | - |
| `fractal.notify.sms.twilio.auth-token` | Twilio Auth Token | Yes | - |
| `fractal.notify.sms.twilio.from-number` | Twilio phone number (E.164 format) | Yes | - |

### WhatsApp Configuration Properties

| Property | Description | Required | Default |
|----------|-------------|----------|---------|
| `fractal.notify.whatsapp.provider` | Provider name | Yes | `default` |
| `fractal.notify.whatsapp.enabled` | Enable WhatsApp | No | `false` |

---

## Troubleshooting

### Issue: "Twilio not configured"

**Solution:**
1. Check `application.properties` has all Twilio credentials
2. Verify environment variables are set (if using `${TWILIO_ACCOUNT_SID}`)
3. Restart application after adding credentials

### Issue: "Invalid phone number format"

**Solution:**
- Use E.164 format: `+[country code][number]`
- Example: `+1234567890` (US), `+919876543210` (India)
- Remove spaces, dashes, parentheses

### Issue: "Template not found"

**Solution:**
1. Check template exists in database:
   ```sql
   SELECT * FROM templates 
   WHERE name = 'your-template-name' 
   AND notification_type = 'SMS' 
   AND is_active = true;
   ```
2. Verify template name matches exactly (case-sensitive)

### Issue: SMS/WhatsApp not sending

**Check:**
1. Twilio account has sufficient credits
2. Phone number is verified (for trial accounts)
3. From number is correct and active
4. Check application logs for detailed error messages

### Issue: "No strategy found for notification type"

**Solution:**
- Make sure `fractal.notify.enabled=true` in `application.properties`
- Verify component scanning includes `com.fractal.notify` package

---

## Twilio Account Setup

### Getting Twilio Credentials

1. **Sign up**: Go to [twilio.com](https://www.twilio.com/try-twilio)
2. **Get Account SID & Auth Token**:
   - Dashboard → Account Info
   - Copy Account SID and Auth Token
3. **Get Phone Number**:
   - Phone Numbers → Buy a number
   - For SMS: Any Twilio number
   - For WhatsApp: Enable WhatsApp on your number (requires approval)

### Testing with Trial Account

Trial accounts can only send to verified numbers:
1. Go to Twilio Console → Phone Numbers → Verified Caller IDs
2. Add your phone number
3. Verify via SMS/call
4. Now you can send SMS to that number

---

## Advanced Usage

### Custom From Number Per Message

```java
// Override default from number for specific message
notificationUtils.sms()
    .to("+1234567890")
    .from("+1987654321")  // Different from number
    .body("Message from custom number")
    .send();
```

### Batch Processing

```java
List<String> phoneNumbers = getPhoneNumbers();
List<String> messages = getMessages();

for (int i = 0; i < phoneNumbers.size(); i++) {
    notificationUtils.sms()
        .to(phoneNumbers.get(i))
        .body(messages.get(i))
        .send();
}
```

### Rate Limiting

Twilio has rate limits. For high-volume sending:
1. Enable queue mode:
   ```properties
   fractal.notify.queue.enabled=true
   ```
2. Notifications will be queued and sent gradually

---

## Response Object Details

### NotificationResponse Fields

```java
NotificationResponse response = notificationUtils.sms()
    .to("+1234567890")
    .body("Test")
    .sendSync();

// Response fields:
response.isSuccess()              // boolean - true if sent successfully
response.getMessageId()          // String - Twilio Message SID (e.g., "SMxxxxxxxx...")
response.getProvider()           // String - "twilio"
response.getNotificationType()   // NotificationType - SMS or WHATSAPP
response.getErrorMessage()       // String - Error message if failed
response.getMessage()             // String - Optional message
```

### Twilio Message SID Format

- SMS: `SMxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx` (34 characters)
- WhatsApp: Similar format

You can use this SID to:
- Track message status via Twilio API
- Check delivery status
- Retrieve message details

---

## Example: Complete REST Controller

```java
package com.yourpackage.controller;

import com.fractal.notify.NotificationUtils;
import com.fractal.notify.core.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationUtils notificationUtils;
    
    @PostMapping("/sms")
    public ResponseEntity<Map<String, Object>> sendSMS(
            @RequestParam String to,
            @RequestParam String body) {
        
        CompletableFuture<NotificationResponse> future = notificationUtils.sms()
            .to(to)
            .body(body)
            .send();
        
        return ResponseEntity.accepted().body(Map.of(
            "status", "queued",
            "message", "SMS is being sent"
        ));
    }
    
    @PostMapping("/sms/sync")
    public ResponseEntity<Map<String, Object>> sendSMSSync(
            @RequestParam String to,
            @RequestParam String body) {
        
        NotificationResponse response = notificationUtils.sms()
            .to(to)
            .body(body)
            .sendSync();
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "messageId", response.getMessageId()
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", response.getErrorMessage()
            ));
        }
    }
    
    @PostMapping("/whatsapp")
    public ResponseEntity<Map<String, Object>> sendWhatsApp(
            @RequestParam String to,
            @RequestParam String body) {
        
        CompletableFuture<NotificationResponse> future = notificationUtils.whatsapp()
            .to(to)
            .body(body)
            .send();
        
        return ResponseEntity.accepted().body(Map.of(
            "status", "queued",
            "message", "WhatsApp message is being sent"
        ));
    }
}
```

---

## Quick Reference

### SMS - Minimal Example

```java
@Autowired
private NotificationUtils notificationUtils;

// Send SMS
notificationUtils.sms()
    .to("+1234567890")
    .body("Hello from Twilio!")
    .send();
```

### WhatsApp - Minimal Example

```java
@Autowired
private NotificationUtils notificationUtils;

// Send WhatsApp
notificationUtils.whatsapp()
    .to("+1234567890")
    .body("Hello from WhatsApp!")
    .send();
```

---

## Support

For issues or questions:
- Check application logs for detailed error messages
- Verify Twilio credentials are correct
- Ensure phone numbers are in E.164 format
- Check Twilio console for account status and credits

---

**Happy Messaging! 📱**
