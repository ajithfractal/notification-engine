package com.fractal.notify.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the notification engine.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "fractal.notify")
public class NotificationProperties {
    private boolean enabled = true;
    private AsyncConfig async = new AsyncConfig();
    private EmailConfig email = new EmailConfig();
    private SMSConfig sms = new SMSConfig();
    private WhatsAppConfig whatsapp = new WhatsAppConfig();
    private PersistenceConfig persistence = new PersistenceConfig();
    private QueueConfig queue = new QueueConfig();
    private StorageConfig storage = new StorageConfig();

    @Getter
    @Setter
    public static class AsyncConfig {
        private String mode = "async";
        private boolean enabled = true;
        private int corePoolSize = 5;
        private int maxPoolSize = 10;
        private int queueCapacity = 100;
    }

    @Getter
    @Setter
    public static class EmailConfig {
        private String provider = "smtp";
        private SMTPConfig smtp = new SMTPConfig();
    }

    @Getter
    @Setter
    public static class SMTPConfig {
        private String host;
        private int port = 587;
        private String username;
        private String password;
        private String from;
        private String replyTo;
    }

    @Getter
    @Setter
    public static class SMSConfig {
        private String provider = "twilio";
        private TwilioConfig twilio = new TwilioConfig();
    }

    @Getter
    @Setter
    public static class TwilioConfig {
        private String accountSid;
        private String authToken;
        private String fromNumber;
    }

    @Getter
    @Setter
    public static class WhatsAppConfig {
        private String provider = "twilio";
        private boolean enabled = false;
        private TwilioWhatsAppConfig twilio = new TwilioWhatsAppConfig();
    }
    
    @Getter
    @Setter
    public static class TwilioWhatsAppConfig {
        private String accountSid;
        private String authToken;
        private String whatsappFromNumber;
    }

    @Getter
    @Setter
    public static class PersistenceConfig {
        private boolean enabled = false; // Disabled by default - opt-in feature
        private DataSourceConfig datasource = new DataSourceConfig();
    }

    @Getter
    @Setter
    public static class QueueConfig {
        private boolean enabled = false;
        private long pollInterval = 5000;
        private int batchSize = 10;
        private int maxRetries = 3;
        private long retryDelay = 60000;
    }

    @Getter
    @Setter
    public static class DataSourceConfig {
        private String url;
        private String username;
        private String password;
        private String driverClassName;
    }

    @Getter
    @Setter
    public static class StorageConfig {
        private String provider = "azure-blob";
        private AzureBlobConfig azureBlob = new AzureBlobConfig();
    }

    @Getter
    @Setter
    public static class AzureBlobConfig {
        private String connectionString;
        private String containerName;
    }
}
