# Thymeleaf Template Guide

## Creating Thymeleaf Templates

### 1. Template Location

**Library Templates** (included in the JAR):
```
src/main/resources/templates/
    ├── email/
    │   ├── welcome.html
    │   ├── order-confirmation.html
    │   └── password-reset.html
    ├── sms/
    │   └── otp.html
    └── whatsapp/
        └── notification.html
```

**Client Application Templates** (in your project):
```
src/main/resources/templates/
    └── email/
        └── custom-template.html
```

### 2. Basic Thymeleaf Syntax

#### Variable Substitution
```html
<!-- Simple text replacement -->
<p>Hello <span th:text="${name}">User</span>!</p>

<!-- Using th:utext for HTML content -->
<div th:utext="${htmlContent}">Default HTML</div>
```

#### Conditional Rendering
```html
<!-- If condition -->
<div th:if="${user.isActive}">
    <p>User is active</p>
</div>

<!-- If-else -->
<div th:if="${user.isPremium}">
    <p>Premium User</p>
</div>
<div th:unless="${user.isPremium}">
    <p>Regular User</p>
</div>
```

#### Loops
```html
<!-- Iterate over a list -->
<ul>
    <li th:each="item : ${items}" th:text="${item.name}">Item</li>
</ul>

<!-- With index -->
<ul>
    <li th:each="item, iterStat : ${items}">
        <span th:text="${iterStat.index}">0</span>. 
        <span th:text="${item.name}">Item</span>
    </li>
</ul>
```

#### Expressions
```html
<!-- String concatenation -->
<p th:text="'Welcome, ' + ${name} + '!'">Welcome!</p>

<!-- Method calls -->
<p th:text="${user.getFullName()}">Full Name</p>

<!-- Safe navigation (null-safe) -->
<p th:text="${user?.address?.city}">City</p>

<!-- Default values -->
<p th:text="${name ?: 'Guest'}">Guest</p>
```

### 3. Email Template Best Practices

#### Email-Safe HTML
- Use inline CSS (not external stylesheets)
- Use table-based layouts for better email client compatibility
- Avoid complex CSS (many email clients don't support it)
- Test in multiple email clients

#### Example Email Template Structure
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${subject}">Email Title</title>
    <style>
        /* Inline styles for email compatibility */
        body {
            font-family: Arial, sans-serif;
            line-height: 1.6;
            color: #333;
            max-width: 600px;
            margin: 0 auto;
            padding: 20px;
        }
        .header {
            background-color: #4CAF50;
            color: white;
            padding: 20px;
            text-align: center;
        }
        .content {
            background-color: #f9f9f9;
            padding: 20px;
        }
        .button {
            display: inline-block;
            padding: 12px 24px;
            background-color: #4CAF50;
            color: white;
            text-decoration: none;
            border-radius: 4px;
        }
        .footer {
            text-align: center;
            margin-top: 20px;
            color: #666;
            font-size: 12px;
        }
    </style>
</head>
<body>
    <div class="header">
        <h1 th:text="${headerTitle}">Header</h1>
    </div>
    <div class="content">
        <p>Hello <strong th:text="${name}">User</strong>,</p>
        <p th:text="${message}">Message content</p>
        
        <!-- Conditional button -->
        <div th:if="${buttonUrl}">
            <a th:href="${buttonUrl}" class="button" th:text="${buttonText}">Click Here</a>
        </div>
    </div>
    <div class="footer">
        <p>This is an automated message. Please do not reply.</p>
    </div>
</body>
</html>
```

### 4. Creating a New Template

**Step 1: Create the HTML file**
```bash
# In your project
src/main/resources/templates/email/order-confirmation.html
```

**Step 2: Write the template**
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Order Confirmation</title>
    <style>
        body { font-family: Arial, sans-serif; }
        .order-details { background: #f5f5f5; padding: 15px; margin: 10px 0; }
    </style>
</head>
<body>
    <h1>Order Confirmation</h1>
    <p>Hello <span th:text="${customerName}">Customer</span>,</p>
    <p>Your order <strong th:text="${orderId}">#12345</strong> has been confirmed.</p>
    
    <div class="order-details">
        <h3>Order Details:</h3>
        <p>Total: <span th:text="${totalAmount}">$0.00</span></p>
        <p>Items:</p>
        <ul>
            <li th:each="item : ${items}" th:text="${item.name + ' - $' + item.price}">Item</li>
        </ul>
    </div>
</body>
</html>
```

**Step 3: Use it in code**
```java
notificationUtils.email()
    .to("customer@example.com")
    .subject("Order Confirmation")
    .template("order-confirmation")
    .variable("customerName", "John Doe")
    .variable("orderId", "ORD-12345")
    .variable("totalAmount", "$99.99")
    .variable("items", Arrays.asList(
        Map.of("name", "Product 1", "price", "49.99"),
        Map.of("name", "Product 2", "price", "50.00")
    ))
    .send();
```

---

## Viewing/Testing Templates

### Method 1: Static Preview (Browser)

Create a test HTML file with sample data:

**test-template.html:**
```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Template Preview</title>
    <style>
        /* Copy styles from your template */
        body { font-family: Arial, sans-serif; }
    </style>
</head>
<body>
    <!-- Replace Thymeleaf expressions with actual values -->
    <h1>Welcome to <span>Acme Corp</span>!</h1>
    <p>Hello <strong>John Doe</strong>,</p>
    <p>We're excited to have you on board!</p>
</body>
</html>
```

Open in browser to preview the design.

### Method 2: Thymeleaf Test Controller (Recommended)

**Step 1:** Add web dependency to your `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

**Step 2:** Create a test controller in your application:

```java
package com.yourpackage.test;

import com.fractal.notify.core.NotificationType;
import com.fractal.notify.template.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class TemplatePreviewController {
    
    @Autowired
    private TemplateService templateService;
    
    @GetMapping("/preview/template")
    @ResponseBody
    public String previewTemplate(
            @RequestParam(defaultValue = "email") String type,
            @RequestParam String templateName) {
        
        // Sample data
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "John Doe");
        variables.put("company", "Acme Corp");
        variables.put("orderId", "ORD-12345");
        variables.put("totalAmount", "$99.99");
        
        NotificationType notificationType = NotificationType.valueOf(type.toUpperCase());
        return templateService.renderTemplate(
            notificationType, 
            templateName, 
            null, 
            variables
        );
    }
}
```

**Step 3:** Access in browser:
```
http://localhost:8080/preview/template?type=email&templateName=welcome
```

### Method 3: Unit Test

```java
package com.fractal.notify.test;

import com.fractal.notify.core.NotificationType;
import com.fractal.notify.template.TemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class TemplateTest {
    
    @Autowired
    private TemplateService templateService;
    
    @Test
    public void testWelcomeTemplate() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "John Doe");
        variables.put("company", "Acme Corp");
        
        String rendered = templateService.renderTemplate(
            NotificationType.EMAIL,
            "welcome",
            null,
            variables
        );
        
        System.out.println("Rendered Template:");
        System.out.println(rendered);
        
        // Save to file for viewing
        // Files.write(Paths.get("output.html"), rendered.getBytes());
    }
}
```

### Method 4: Command Line Test

Create a simple test class:

```java
package com.fractal.notify.test;

import com.fractal.notify.template.TemplateService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class TemplatePreviewRunner implements CommandLineRunner {
    
    private final TemplateService templateService;
    
    public TemplatePreviewRunner(TemplateService templateService) {
        this.templateService = templateService;
    }
    
    @Override
    public void run(String... args) {
        if (args.length > 0 && args[0].equals("preview")) {
            Map<String, Object> vars = Map.of(
                "name", "John Doe",
                "company", "Acme Corp"
            );
            
            String rendered = templateService.renderTemplate(
                NotificationType.EMAIL,
                "welcome",
                null,
                vars
            );
            
            System.out.println(rendered);
        }
    }
}
```

Run: `java -jar app.jar preview`

---

## Common Thymeleaf Patterns for Emails

### 1. Dynamic Content
```html
<p th:text="'Hello ' + ${firstName} + ' ' + ${lastName}">Hello User</p>
```

### 2. Conditional Content
```html
<div th:if="${orderStatus == 'SHIPPED'}">
    <p>Your order has been shipped!</p>
</div>
<div th:if="${orderStatus == 'PENDING'}">
    <p>Your order is being processed.</p>
</div>
```

### 3. Lists/Arrays
```html
<ul>
    <li th:each="product : ${products}" th:text="${product.name}">Product</li>
</ul>
```

### 4. Dates
```html
<p th:text="${#temporals.format(orderDate, 'dd MMM yyyy')}">01 Jan 2024</p>
```

### 5. Numbers/Currency
```html
<p th:text="${#numbers.formatDecimal(total, 2, 2)}">99.99</p>
<p th:text="'$' + ${#numbers.formatDecimal(amount, 2, 2)}">$99.99</p>
```

### 6. Links
```html
<a th:href="${resetLink}" th:text="'Reset Password'">Reset Password</a>
```

### 7. Images
```html
<img th:src="${logoUrl}" alt="Company Logo" />
```

---

## Tips for Email Templates

1. **Use Inline CSS**: Email clients strip out `<style>` tags, so use inline styles
2. **Table Layouts**: Use tables for layout (better email client support)
3. **Test in Multiple Clients**: Gmail, Outlook, Apple Mail, etc.
4. **Keep It Simple**: Avoid complex CSS and JavaScript
5. **Mobile Responsive**: Use media queries for mobile devices
6. **Alt Text for Images**: Always provide alt text
7. **Fallback Colors**: Provide fallback colors for background

---

## Example: Complete Email Template

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Password Reset</title>
</head>
<body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
    
    <!-- Header -->
    <div style="background-color: #4CAF50; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0;">
        <h1 style="margin: 0;">Password Reset Request</h1>
    </div>
    
    <!-- Content -->
    <div style="background-color: #f9f9f9; padding: 20px; border-radius: 0 0 5px 5px;">
        <p>Hello <strong th:text="${name}">User</strong>,</p>
        
        <p>You requested to reset your password. Click the button below to reset it:</p>
        
        <div style="text-align: center; margin: 30px 0;">
            <a th:href="${resetLink}" 
               style="display: inline-block; padding: 12px 24px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 4px;">
                Reset Password
            </a>
        </div>
        
        <p style="font-size: 12px; color: #666;">
            If you didn't request this, please ignore this email. 
            This link will expire in <span th:text="${expiryHours}">24</span> hours.
        </p>
    </div>
    
    <!-- Footer -->
    <div style="text-align: center; margin-top: 20px; color: #666; font-size: 12px;">
        <p>This is an automated message. Please do not reply to this email.</p>
        <p th:text="${companyName}">Company Name</p>
    </div>
    
</body>
</html>
```

---

## Quick Reference

| Thymeleaf Expression | Description |
|---------------------|-------------|
| `th:text="${var}"` | Insert text (escaped) |
| `th:utext="${var}"` | Insert HTML (unescaped) |
| `th:if="${condition}"` | Conditional rendering |
| `th:each="item : ${list}"` | Loop through list |
| `th:href="${url}"` | Set href attribute |
| `th:src="${imageUrl}"` | Set src attribute |
| `${var ?: 'default'}` | Default value if null |
| `${user?.name}` | Safe navigation (null-safe) |
| `${#temporals.format(date)}` | Format date |
| `${#numbers.formatDecimal(num)}` | Format number |

---

## Next Steps

1. Create your template in `src/main/resources/templates/email/`
2. Test it using one of the methods above
3. Use it in your code with `notificationUtils.email().template("your-template")`
4. Pass variables using `.variable("key", value)`
