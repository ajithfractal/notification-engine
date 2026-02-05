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
        private String provider = "default";
        private boolean enabled = false;
    }

    @Getter
    @Setter
    public static class PersistenceConfig {
        private boolean enabled = false; // Disabled by default - opt-in feature
        private DataSourceConfig datasource = new DataSourceConfig();
    }

    @Getter
    @Setter
    public static class DataSourceConfig {
        private String url;
        private String username;
        private String password;
        private String driverClassName;
    }
}
