# Quick Start - Using Fractal Notification Engine

## 1. Install the Dependency

### Build and Install Locally

```bash
cd fractal-notify
mvn clean install
```

### Add to Your Project

**Maven (pom.xml)**:
```xml
<dependency>
    <groupId>com.fractal</groupId>
    <artifactId>fractal-notify</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Gradle (build.gradle)**:
```gradle
dependencies {
    implementation 'com.fractal:fractal-notify:1.0.0'
}
```

## 2. Enable Component Scanning

In your Spring Boot application class:

```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.fractal.notify", "your.package"})
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

## 3. Configure (application.yml)

```yaml
fractal:
  notify:
    email:
      smtp:
        host: smtp.gmail.com
        port: 587
        username: ${EMAIL_USERNAME}
        password: ${EMAIL_PASSWORD}
        from: noreply@yourcompany.com
    sms:
      twilio:
        account-sid: ${TWILIO_ACCOUNT_SID}
        auth-token: ${TWILIO_AUTH_TOKEN}
        from-number: ${TWILIO_FROM_NUMBER}
```

## 4. Use It!

```java
@Autowired
private NotificationUtils notificationUtils;

// Send email
notificationUtils.email()
    .to("user@example.com")
    .subject("Welcome")
    .body("Welcome to our platform!")
    .send();

// Send SMS
notificationUtils.sms()
    .to("+1234567890")
    .body("Your OTP is: 123456")
    .send();
```

**That's it!** You're ready to send notifications.

For more details, see [USAGE_GUIDE.md](USAGE_GUIDE.md)
