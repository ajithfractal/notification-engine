package com.fractal.notify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the notification engine.
 */
@Data
@ConfigurationProperties(prefix = "fractal.notify")
public class NotificationProperties {
    private boolean enabled = true;
    private AsyncConfig async = new AsyncConfig();
    private EmailConfig email = new EmailConfig();
    private SMSConfig sms = new SMSConfig();
    private WhatsAppConfig whatsapp = new WhatsAppConfig();
    private PersistenceConfig persistence = new PersistenceConfig();
    private QueueConfig queue = new QueueConfig();

    @Data
    public static class AsyncConfig {
        private String mode = "async";
        private boolean enabled = true;
        private int corePoolSize = 5;
        private int maxPoolSize = 10;
        private int queueCapacity = 100;
    }

    @Data
    public static class EmailConfig {
        private String provider = "smtp";
        private SMTPConfig smtp = new SMTPConfig();
    }

    @Data
    public static class SMTPConfig {
        private String host;
        private int port = 587;
        private String username;
        private String password;
        private String from;
    }

    @Data
    public static class SMSConfig {
        private String provider = "twilio";
        private TwilioConfig twilio = new TwilioConfig();
    }

    @Data
    public static class TwilioConfig {
        private String accountSid;
        private String authToken;
        private String fromNumber;
    }

    @Data
    public static class WhatsAppConfig {
        private String provider = "default";
        private boolean enabled = false;
    }

    @Data
    public static class PersistenceConfig {
        private boolean enabled = true;
        private DataSourceConfig datasource = new DataSourceConfig();
    }

    @Data
    public static class QueueConfig {
        private boolean enabled = false;
        private long pollInterval = 5000;
        private int batchSize = 10;
        private int maxRetries = 3;
        private long retryDelay = 60000;
    }

    @Data
    public static class DataSourceConfig {
        private String url;
        private String username;
        private String password;
        private String driverClassName;
    }
}
