# WhatsApp Template Usage Guide

## Understanding Twilio Content Templates

Twilio WhatsApp Content Templates use **numbered placeholders** like `{{1}}`, `{{2}}`, `{{3}}`, etc.

When sending messages, you need to map your variables to these numbered placeholders.

## Your Welcome Template

Your template structure:
```
👋 *Welcome to {{1}}!*

Hello {{2}},

We are delighted to welcome you to *{{1}}*. Your account has been successfully created, and you are now part of our platform. 🎉

If you need any assistance or have questions, our support team is always happy to help.

Kind regards,  
*{{1}} Team*
```

**Variable Mapping:**
- `{{1}}` = Company Name (used 3 times in the template)
- `{{2}}` = User/Customer Name

## Step 1: Insert Template into Database

First, get your **Content Template SID** from Twilio Console:
1. Go to [Twilio Console](https://console.twilio.com)
2. Navigate to **Content** → **Content Templates**
3. Find your approved WhatsApp template
4. Copy the **SID** (starts with `HX...`)

Then run this SQL (replace `HX...` with your actual SID):

```sql
INSERT INTO public.templates (
    name,
    notification_type,
    provider,
    content_sid,
    content,
    is_active,
    created_at,
    updated_at
)
VALUES (
    'WELCOME_WHATSAPP',
    'WHATSAPP',
    'TWILIO',
    'HX...your-actual-content-template-sid...',  -- Your Twilio Content Template SID
    '👋 *Welcome to {{1}}!*

Hello {{2}},

We are delighted to welcome you to *{{1}}*. Your account has been successfully created, and you are now part of our platform. 🎉

If you need any assistance or have questions, our support team is always happy to help.

Kind regards,  
*{{1}} Team*',
    true,
    now(),
    now()
)
ON CONFLICT (name, notification_type) DO NOTHING;
```

## Step 2: Use in Your Code

### Option 1: Using NotificationUtils (Recommended)

```java
@Autowired
private NotificationUtils notificationUtils;

// Send welcome WhatsApp message
notificationUtils.whatsapp()
    .to("+918921172797")  // Recipient phone number
    .template("WELCOME_WHATSAPP")
    .variable("1", "Fractal")        // {{1}} = Company Name
    .variable("2", "Ajith")          // {{2}} = User Name
    .send();
```

**Important:** The variable keys must be `"1"`, `"2"`, etc. (as strings) to match Twilio's `{{1}}`, `{{2}}` format.

### Option 2: Direct Service Call

```java
@Autowired
private NotificationService notificationService;

NotificationRequest request = NotificationRequest.builder()
    .notificationType(NotificationType.WHATSAPP)
    .to(List.of("+918921172797"))
    .templateName("WELCOME_WHATSAPP")
    .templateVariables(Map.of(
        "1", "Fractal",    // {{1}} = Company Name
        "2", "Ajith"       // {{2}} = User Name
    ))
    .build();

NotificationResponse response = notificationService.send(request);
```

## Variable Mapping Reference

For your welcome template:

| Twilio Placeholder | Variable Key | Description | Example Value |
|-------------------|--------------|-------------|---------------|
| `{{1}}` | `"1"` | Company Name | `"Fractal"` |
| `{{2}}` | `"2"` | User Name | `"Ajith"` |

## Complete Example

```java
@Service
public class WelcomeService {
    
    @Autowired
    private NotificationUtils notificationUtils;
    
    public void sendWelcomeMessage(String phoneNumber, String userName, String companyName) {
        notificationUtils.whatsapp()
            .to(phoneNumber)
            .template("WELCOME_WHATSAPP")
            .variable("1", companyName)  // Maps to {{1}} in template
            .variable("2", userName)     // Maps to {{2}} in template
            .send()
            .thenAccept(response -> {
                if (response.isSuccess()) {
                    log.info("Welcome WhatsApp sent successfully! Message ID: {}", response.getMessageId());
                } else {
                    log.error("Failed to send welcome WhatsApp: {}", response.getErrorMessage());
                }
            })
            .exceptionally(throwable -> {
                log.error("Exception sending welcome WhatsApp", throwable);
                return null;
            });
    }
}
```

## Important Notes

1. **Content Template SID**: Must start with `HX...` (not `AC...` which is Account SID)
2. **Template Approval**: Your template must be **approved** in Twilio Console before use
3. **Variable Keys**: Use `"1"`, `"2"`, etc. (as strings) to match `{{1}}`, `{{2}}` placeholders
4. **Phone Format**: Always use E.164 format: `+[country code][number]` (e.g., `+918921172797`)

## Troubleshooting

### Error: "Invalid Content Template SID"
- **Cause**: SID doesn't start with `HX`
- **Fix**: Get the correct Content Template SID from Twilio Console

### Error: "Template not found"
- **Cause**: Template name doesn't match or template is not active
- **Fix**: Check database: `SELECT * FROM templates WHERE name = 'WELCOME_WHATSAPP' AND is_active = true;`

### Error: "Invalid Parameter" from Twilio
- **Cause**: Content Template not approved or SID is incorrect
- **Fix**: Verify template is approved in Twilio Console and SID is correct

### Variables not replacing
- **Cause**: Variable keys don't match template placeholders
- **Fix**: Use `"1"`, `"2"` as keys (not `"companyName"`, `"userName"`)

## Next Steps

1. ✅ Get your Content Template SID from Twilio Console
2. ✅ Insert template into database with correct SID
3. ✅ Use the code examples above to send messages
4. ✅ Test with a verified WhatsApp number first
