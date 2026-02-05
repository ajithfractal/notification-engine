# Database Setup Guide

## Quick Setup (Development)

Add to your `application.properties`:

```properties
# Enable persistence
fractal.notify.persistence.enabled=true

# Database configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/your_database
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver

# Auto-create tables (Development only - NOT for production!)
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

## Manual Table Creation (Production Recommended)

Execute this SQL script in your PostgreSQL database:

```sql
-- Create notifications table for persisting notification requests
CREATE TABLE IF NOT EXISTS notifications (
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

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_notification_status ON notifications(status);
CREATE INDEX IF NOT EXISTS idx_notification_created_at ON notifications(created_at);
CREATE INDEX IF NOT EXISTS idx_notification_type ON notifications(notification_type);
```

## Using Flyway (Recommended for Production)

1. Add Flyway dependency to your client application's `pom.xml`:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

2. Copy the migration script from the library:
   - Source: `src/main/resources/db/migration/V1__create_notification_table.sql`
   - Destination: Your app's `src/main/resources/db/migration/V1__create_notification_table.sql`

3. Configure Flyway in `application.properties`:

```properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
```

## Verification

After setup, verify the table exists:

```sql
SELECT * FROM notifications LIMIT 1;
```
