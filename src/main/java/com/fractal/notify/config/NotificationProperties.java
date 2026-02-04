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

    @Data
    public static class AsyncConfig {
        private String mode = "async"; // Options: async, kafka (kafka for future use)
        private boolean enabled = true;
        private int corePoolSize = 5;
        private int maxPoolSize = 10;
        private int queueCapacity = 100;
        private KafkaConfig kafka = new KafkaConfig();
    }

    @Data
    public static class KafkaConfig {
        private boolean enabled = false; // Disabled by default, enable when Kafka is needed
        private String topic = "notifications";
        private String consumerGroup = "notification-processors";
        private int partitions = 3;
        private int replicationFactor = 1;
        private int concurrency = 3; // Number of consumer threads
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
}
