# Static Assets Guide

## Overview

Static assets (logos, headers, footers, etc.) are stored in the database and automatically referenced in email templates via URLs. They are **NOT attachments** - they're embedded inline images referenced directly in HTML.

## How It Works

1. **Store static assets** in the `static_assets` table
2. **Reference them in HTML** using `cid:` syntax: `<img src='cid:logo'>`
3. **System automatically replaces** `cid:` references with direct URLs to storage
4. **No attachments** - images load from URLs, not attached files

## Database Setup

### Create Static Asset

```sql
INSERT INTO static_assets (name, storage_path, file_name, content_type, content_id, is_active, description)
VALUES 
    ('company-logo', 'static-assets/logo.png', 'logo.png', 'image/png', 'logo', true, 'Company logo'),
    ('email-header', 'static-assets/header.png', 'header.png', 'image/png', 'header', true, 'Email header'),
    ('email-footer', 'static-assets/footer.png', 'footer.png', 'image/png', 'footer', true, 'Email footer');
```

### Table Structure

- `name`: Unique identifier (e.g., "company-logo")
- `storage_path`: Path in blob storage (e.g., "static-assets/logo.png")
- `public_url`: Optional pre-configured public URL
- `file_name`: Display name
- `content_type`: MIME type (e.g., "image/png")
- `content_id`: CID reference used in HTML (e.g., "logo" for `cid:logo`)
- `is_active`: Enable/disable asset

## Usage in Email Templates

### In HTML Body

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

### In Code

```java
String htmlBody = """
    <html>
    <body>
        <img src='cid:logo' alt='Logo' />
        <h1>Welcome!</h1>
    </body>
    </html>
    """;

notificationUtils.email()
    .to("user@example.com")
    .subject("Welcome")
    .body(htmlBody)  // cid: references automatically replaced with URLs
    .send();
```

## What Happens

1. You write HTML with `cid:logo` reference
2. System scans HTML for `cid:` patterns
3. Looks up static asset by `content_id` in database
4. Generates public URL from storage path (or uses `public_url` if set)
5. Replaces `cid:logo` with actual URL: `https://storage.../logo.png`
6. Email is sent with images loading from URLs (not attachments)

## Benefits

- ✅ **No attachments** - Images load from URLs
- ✅ **No duplicate uploads** - Assets stored once, referenced many times
- ✅ **Easy updates** - Change logo in DB, all emails use new version
- ✅ **Faster emails** - Smaller message size
- ✅ **Better caching** - Browsers cache images

## Storage Path Format

Static assets should be uploaded to a dedicated folder in blob storage:

```
Azure Blob Storage:
├── static-assets/
│   ├── logo.png
│   ├── header.png
│   └── footer.png
└── email-attachments/
    └── [dynamic attachments]
```

## Public URLs

The system automatically generates public URLs (SAS URLs) for static assets. URLs are valid for 1 year.

If you have pre-configured public URLs, set the `public_url` field in the database to use those instead.

## Example: Complete Email with Static Assets

```java
// Template stored in database or passed as body
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
    .body(htmlBody)
    .variable("name", "John")
    .send();
```

## Notes

- Static assets are **only for email** notifications
- They work automatically - no code changes needed
- If asset not found, original `cid:` reference is kept (won't break HTML)
- Assets must be uploaded to storage first before adding to database
- Use `content_id` to match `cid:` references in HTML
