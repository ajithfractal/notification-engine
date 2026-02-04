# How to Use Fractal Notification Engine as a Dependency

## Step 1: Add Dependency to Your Project

### Maven (pom.xml)

```xml
<dependency>
    <groupId>com.fractal</groupId>
    <artifactId>fractal-notify</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle (build.gradle)

```gradle
dependencies {
    implementation 'com.fractal:fractal-notify:1.0.0'
}
```

## Step 2: Enable Component Scanning

In your Spring Boot application, make sure to scan the `com.fractal.notify` package:

```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.fractal.notify", "your.package.name"})
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

**OR** if you want to scan all packages (default behavior):

```java
@SpringBootApplication
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

The auto-configuration will be picked up automatically via `spring.factories`.

## Step 3: Configure in application.yml

```yaml
fractal:
  notify:
    enabled: true  # Default is true, can be omitted
    async:
      mode: async  # Options: async (current), kafka (future)
      core-pool-size: 5
      max-pool-size: 10
      queue-capacity: 100
    
    # Email Configuration
    email:
      provider: smtp
      smtp:
        host: smtp.gmail.com
        port: 587
        username: ${EMAIL_USERNAME}
        password: ${EMAIL_PASSWORD}
        from: noreply@yourcompany.com
    
    # SMS Configuration
    sms:
      provider: twilio
      twilio:
        account-sid: ${TWILIO_ACCOUNT_SID}
        auth-token: ${TWILIO_AUTH_TOKEN}
        from-number: ${TWILIO_FROM_NUMBER}
```

## Step 4: Use NotificationUtils

### Inject and Use

```java
import com.fractal.notify.NotificationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    
    @Autowired
    private NotificationUtils notificationUtils;
    
    public void sendWelcomeEmail(String email, String name) {
        notificationUtils.email()
            .to(email)
            .subject("Welcome to Our Platform")
            .template("welcome")
            .variable("name", name)
            .send();
    }
    
    public void sendOTP(String phoneNumber, String otp) {
        notificationUtils.sms()
            .to(phoneNumber)
            .body("Your OTP is: " + otp)
            .send();
    }
}
```

### Direct Usage with NotificationRequest

```java
import com.fractal.notify.core.NotificationRequest;
import com.fractal.notify.core.NotificationType;
import com.fractal.notify.NotificationUtils;

@Service
public class OrderService {
    
    @Autowired
    private NotificationUtils notificationUtils;
    
    public void sendOrderConfirmation(String email, String orderId) {
        NotificationRequest request = NotificationRequest.builder()
            .notificationType(NotificationType.EMAIL)
            .to(email)
            .subject("Order Confirmation")
            .body("Your order " + orderId + " has been confirmed.")
            .build();
        
        notificationUtils.sendAsync(request);
    }
}
```

## Step 5: Add Email Templates (Optional)

If you want to use templates, place them in your project's `src/main/resources/templates/email/`:

```
src/main/resources/templates/email/
    ├── welcome.html
    ├── order-confirmation.html
    └── password-reset.html
```

Example template (`welcome.html`):

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Welcome</title>
</head>
<body>
    <h1>Welcome, <span th:text="${name}">User</span>!</h1>
    <p>Thank you for joining us.</p>
</body>
</html>
```

## Complete Example

### Application Class

```java
package com.example.myapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### Service Class

```java
package com.example.myapp.service;

import com.fractal.notify.NotificationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    
    @Autowired
    private NotificationUtils notificationUtils;
    
    public void notifyUser(String email, String name) {
        notificationUtils.email()
            .to(email)
            .subject("Welcome")
            .template("welcome")
            .variable("name", name)
            .send();
    }
}
```

### application.yml

```yaml
fractal:
  notify:
    email:
      smtp:
        host: smtp.gmail.com
        port: 587
        username: ${EMAIL_USERNAME}
        password: ${EMAIL_PASSWORD}
        from: noreply@example.com
```

## Troubleshooting

### Components Not Found

If you get `NoSuchBeanDefinitionException` for `NotificationUtils`:

1. **Check Component Scanning**: Make sure `@ComponentScan` includes `com.fractal.notify`
2. **Check Configuration**: Ensure `fractal.notify.enabled=true` (or omit, defaults to true)
3. **Check Dependencies**: Verify all required Spring Boot dependencies are present

### Auto-Configuration Not Working

If auto-configuration doesn't work:

1. Check that `META-INF/spring.factories` exists in the JAR
2. Verify Spring Boot version compatibility (requires Spring Boot 3.x)
3. Check application logs for auto-configuration messages

### Email Not Sending

1. **Check SMTP Configuration**: Verify host, port, username, password
2. **Check Network**: Ensure SMTP port is not blocked
3. **Check Logs**: Look for error messages in application logs
4. **Test Connection**: Try connecting to SMTP server manually

## Advanced Usage

### Synchronous Sending

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

### Custom From Address

```java
notificationUtils.email()
    .to("user@example.com")
    .from("custom@example.com")
    .subject("Test")
    .body("Test message")
    .send();
```

### Multiple Template Variables

```java
Map<String, Object> variables = new HashMap<>();
variables.put("name", "John");
variables.put("orderId", "12345");
variables.put("amount", "$99.99");

notificationUtils.email()
    .to("user@example.com")
    .subject("Order Confirmation")
    .template("order-confirmation")
    .variables(variables)
    .send();
```
