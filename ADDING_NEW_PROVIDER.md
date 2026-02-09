# Adding a New Provider Guide

This guide explains how to add a new provider (Email, SMS, or WhatsApp) to the Fractal Notify system.

## Overview

The system uses a **Factory Pattern** with automatic provider discovery. You simply need to:
1. Create a provider implementation class
2. Add configuration (if needed)
3. Configure it in `application.properties`

The factory automatically discovers and uses your provider - no manual registration required!

---

## Step-by-Step Guide

### Example: Adding a SendGrid Email Provider

#### Step 1: Create the Provider Implementation

Create a new class that implements the provider interface. For Email, implement `EmailProvider`:

```java
package com.fractal.notify.email.provider;

import com.fractal.notify.config.NotificationProperties;
import com.fractal.notify.email.EmailProvider;
import com.fractal.notify.email.EmailResponse;
import com.fractal.notify.email.dto.EmailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendGridEmailProvider implements EmailProvider {
    private final NotificationProperties properties;
    private static final String PROVIDER_NAME = "sendgrid";
    
    // Add SendGrid SDK client here
    // private final SendGrid sendGrid;

    @Override
    public EmailResponse sendEmail(EmailRequest request) {
        try {
            // Validate configuration
            if (!isConfigured()) {
                return EmailResponse.builder()
                        .success(false)
                        .errorMessage("SendGrid not configured")
                        .build();
            }
            
            // Implement SendGrid API call
            // Example:
            // Mail mail = new Mail();
            // mail.setFrom(new Email(request.getFrom()));
            // mail.setSubject(request.getSubject());
            // mail.addContent(new Content("text/html", request.getBody()));
            // 
            // Personalization personalization = new Personalization();
            // request.getTo().forEach(to -> personalization.addTo(new Email(to)));
            // mail.addPersonalization(personalization);
            //
            // Request sgRequest = new Request();
            // sgRequest.setMethod(Method.POST);
            // sgRequest.setEndpoint("mail/send");
            // sgRequest.setBody(mail.build());
            // Response response = sendGrid.api(sgRequest);
            
            log.info("Email sent successfully via SendGrid to {}", request.getTo());
            return EmailResponse.builder()
                    .success(true)
                    .messageId("sg-" + System.currentTimeMillis()) // Use actual message ID from SendGrid
                    .build();
                    
        } catch (Exception e) {
            log.error("Error sending email via SendGrid", e);
            return EmailResponse.builder()
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
        // Check if SendGrid API key is configured
        // return properties.getEmail().getSendgrid() != null 
        //         && properties.getEmail().getSendgrid().getApiKey() != null;
        return true; // Placeholder
    }
}
```

#### Step 2: Add Configuration Properties (Optional)

If your provider needs configuration, add it to `NotificationProperties.java`:

```java
@Data
public static class EmailConfig {
    private String provider = "smtp";
    private SMTPConfig smtp = new SMTPConfig();
    private SendGridConfig sendgrid = new SendGridConfig(); // Add this
}

@Data
public static class SendGridConfig {
    private String apiKey;
    private String from;
}
```

#### Step 3: Configure in application.properties

Set the provider name in your configuration:

```properties
fractal.notify.email.provider=sendgrid
fractal.notify.email.sendgrid.api-key=your-api-key-here
fractal.notify.email.sendgrid.from=noreply@yourdomain.com
```

---

## For SMS Provider (Example: AWS SNS)

Same pattern, different interface:

```java
package com.fractal.notify.sms.provider;

import com.fractal.notify.config.NotificationProperties;
import com.fractal.notify.sms.SMSProvider;
import com.fractal.notify.sms.SMSResponse;
import com.fractal.notify.sms.dto.SMSRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AWSSNSProvider implements SMSProvider {
    private final NotificationProperties properties;
    private static final String PROVIDER_NAME = "aws-sns";
    
    // private final AmazonSNS snsClient;

    @Override
    public SMSResponse sendSMS(SMSRequest request) {
        try {
            // AWS SNS implementation
            // PublishRequest publishRequest = new PublishRequest()
            //     .withPhoneNumber(request.getTo())
            //     .withMessage(request.getBody());
            // PublishResult result = snsClient.publish(publishRequest);
            
            log.info("SMS sent successfully via AWS SNS to {}", request.getTo());
            return SMSResponse.builder()
                    .success(true)
                    .messageId("aws-" + System.currentTimeMillis())
                    .build();
                    
        } catch (Exception e) {
            log.error("Error sending SMS via AWS SNS", e);
            return SMSResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
```

Then configure:
```properties
fractal.notify.sms.provider=aws-sns
```

---

## Key Points to Remember

1. ✅ **Use `@Component`** - So Spring will auto-detect it
2. ✅ **Implement the correct interface** - `EmailProvider`, `SMSProvider`, or `WhatsAppProvider`
3. ✅ **Return unique provider name** - From `getProviderName()` method (must match config)
4. ✅ **Handle errors gracefully** - Return appropriate response objects
5. ✅ **Add configuration properties** - If your provider needs config
6. ✅ **The factory automatically picks it up** - No manual registration needed!

---

## Testing Your Provider

After adding your provider, test it:

```java
// In your application
@Autowired
private NotificationUtils notificationUtils;

public void testNewProvider() {
    notificationUtils.email()
        .to("test@example.com")
        .subject("Test")
        .body("Hello from SendGrid!")
        .send();
}
```

The system will automatically use your new provider based on the `fractal.notify.email.provider` configuration value.

---

## Summary Checklist

- [ ] Create provider class implementing `EmailProvider`/`SMSProvider`/`WhatsAppProvider`
- [ ] Add `@Component` annotation
- [ ] Implement `sendEmail()`/`sendSMS()` method
- [ ] Implement `getProviderName()` returning unique name
- [ ] Add configuration properties (if needed)
- [ ] Update `application.properties` with provider name
- [ ] Add any required dependencies to `pom.xml` (e.g., SendGrid SDK)

The factory pattern handles the rest automatically!

---

## How It Works

1. **Spring Auto-Discovery**: Your `@Component` annotated provider is automatically discovered by Spring
2. **Factory Pattern**: The `EmailProviderFactory`/`SMSProviderFactory` collects all providers via dependency injection
3. **Configuration-Based Selection**: The factory selects the provider based on `fractal.notify.email.provider` value
4. **Strategy Pattern**: The notification strategy uses the factory to get the correct provider

No manual wiring or registration needed - it's all automatic! 🎉

---

## Existing Provider Examples

For reference, you can look at existing implementations:

- **Email**: `SMTPEmailProvider` in `com.fractal.notify.email.provider`
- **SMS**: `TwilioSMSProvider` in `com.fractal.notify.sms.provider`
- **WhatsApp**: `DefaultWhatsAppProvider` in `com.fractal.notify.whatsapp.provider`

These serve as excellent templates for creating your own provider implementations.
