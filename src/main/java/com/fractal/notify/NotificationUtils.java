package com.fractal.notify;

import com.fractal.notify.core.NotificationRequest;
import com.fractal.notify.core.NotificationResponse;
import com.fractal.notify.core.NotificationService;
import com.fractal.notify.core.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Simple utility class for sending notifications.
 * Provides a clean, easy-to-use API for client applications.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationUtils {
    private final NotificationService notificationService;

    /**
     * Create an email notification builder.
     *
     * @return EmailNotificationBuilder
     */
    public EmailNotificationBuilder email() {
        EmailNotificationBuilder builder = new EmailNotificationBuilder(NotificationType.EMAIL);
        builder.setUtils(this);
        return builder;
    }

    /**
     * Create an SMS notification builder.
     *
     * @return SMSNotificationBuilder
     */
    public SMSNotificationBuilder sms() {
        SMSNotificationBuilder builder = new SMSNotificationBuilder(NotificationType.SMS);
        builder.setUtils(this);
        return builder;
    }

    /**
     * Create a WhatsApp notification builder.
     *
     * @return WhatsAppNotificationBuilder
     */
    public WhatsAppNotificationBuilder whatsapp() {
        WhatsAppNotificationBuilder builder = new WhatsAppNotificationBuilder(NotificationType.WHATSAPP);
        builder.setUtils(this);
        return builder;
    }

    /**
     * Send a notification asynchronously.
     *
     * @param request the notification request
     * @return CompletableFuture with the notification response
     */
    public CompletableFuture<NotificationResponse> sendAsync(NotificationRequest request) {
        return notificationService.sendAsync(request);
    }

    /**
     * Send a notification synchronously.
     *
     * @param request the notification request
     * @return the notification response
     */
    public NotificationResponse send(NotificationRequest request) {
        return notificationService.send(request);
    }

    /**
     * Base builder class for notifications.
     */
    public abstract static class NotificationBuilder {
        protected final NotificationType notificationType;
        protected String to;
        protected String subject;
        protected String body;
        protected String templateName;
        protected Map<String, Object> templateVariables = new HashMap<>();
        protected String from;
        protected NotificationUtils utils;

        protected NotificationBuilder(NotificationType notificationType) {
            this.notificationType = notificationType;
        }

        protected void setUtils(NotificationUtils utils) {
            this.utils = utils;
        }

        public NotificationBuilder to(String to) {
            this.to = to;
            return this;
        }

        public NotificationBuilder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public NotificationBuilder body(String body) {
            this.body = body;
            return this;
        }

        public NotificationBuilder template(String templateName) {
            this.templateName = templateName;
            return this;
        }

        public NotificationBuilder variable(String key, Object value) {
            this.templateVariables.put(key, value);
            return this;
        }

        public NotificationBuilder variables(Map<String, Object> variables) {
            this.templateVariables.putAll(variables);
            return this;
        }

        public NotificationBuilder from(String from) {
            this.from = from;
            return this;
        }

        protected NotificationRequest buildRequest() {
            return NotificationRequest.builder()
                    .notificationType(notificationType)
                    .to(to)
                    .subject(subject)
                    .body(body)
                    .templateName(templateName)
                    .templateVariables(templateVariables.isEmpty() ? null : templateVariables)
                    .from(from)
                    .build();
        }

        public CompletableFuture<NotificationResponse> send() {
            NotificationRequest request = buildRequest();
            if (utils == null) {
                throw new IllegalStateException("NotificationUtils not set. Use notificationUtils.email() to create builder.");
            }
            return utils.sendAsync(request);
        }

        public NotificationResponse sendSync() {
            NotificationRequest request = buildRequest();
            if (utils == null) {
                throw new IllegalStateException("NotificationUtils not set. Use notificationUtils.email() to create builder.");
            }
            return utils.send(request);
        }
    }

    /**
     * Builder for email notifications.
     */
    public static class EmailNotificationBuilder extends NotificationBuilder {
        protected EmailNotificationBuilder(NotificationType type) {
            super(type);
        }
    }

    /**
     * Builder for SMS notifications.
     */
    public static class SMSNotificationBuilder extends NotificationBuilder {
        protected SMSNotificationBuilder(NotificationType type) {
            super(type);
        }
    }

    /**
     * Builder for WhatsApp notifications.
     */
    public static class WhatsAppNotificationBuilder extends NotificationBuilder {
        protected WhatsAppNotificationBuilder(NotificationType type) {
            super(type);
        }
    }
}
