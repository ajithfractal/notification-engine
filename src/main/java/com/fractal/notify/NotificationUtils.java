package com.fractal.notify;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.fractal.notify.core.NotificationRequest;
import com.fractal.notify.core.NotificationResponse;
import com.fractal.notify.core.NotificationService;
import com.fractal.notify.core.NotificationType;
import com.fractal.notify.email.dto.EmailAttachment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
        protected List<String> to;
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

        /**
         * Set a single recipient.
         */
        public NotificationBuilder to(String to) {
            this.to = java.util.Collections.singletonList(to);
            return this;
        }

        /**
         * Set multiple recipients.
         */
        public NotificationBuilder to(String... toRecipients) {
            this.to = java.util.Arrays.asList(toRecipients);
            return this;
        }

        /**
         * Set multiple recipients from a list.
         */
        public NotificationBuilder to(List<String> toRecipients) {
            this.to = toRecipients;
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
        protected List<String> cc;
        protected List<String> bcc;
        protected String replyTo;
        protected List<EmailAttachment> attachments;

        protected EmailNotificationBuilder(NotificationType type) {
            super(type);
        }

        public EmailNotificationBuilder cc(String... ccAddresses) {
            this.cc = Arrays.asList(ccAddresses);
            return this;
        }

        public EmailNotificationBuilder cc(List<String> ccAddresses) {
            this.cc = ccAddresses;
            return this;
        }

        public EmailNotificationBuilder bcc(String... bccAddresses) {
            this.bcc = Arrays.asList(bccAddresses);
            return this;
        }

        public EmailNotificationBuilder bcc(List<String> bccAddresses) {
            this.bcc = bccAddresses;
            return this;
        }

        /**
         * Set Reply-To address.
         * If not set, will use the value from application properties (fractal.notify.email.smtp.reply-to).
         */
        public EmailNotificationBuilder replyTo(String replyTo) {
            this.replyTo = replyTo;
            return this;
        }

        /**
         * Add a file attachment.
         *
         * @param file the file to attach
         * @return this builder
         */
        public EmailNotificationBuilder attachment(File file) {
            if (attachments == null) {
                attachments = new ArrayList<>();
            }
            attachments.add(EmailAttachment.builder()
                    .file(file)
                    .build());
            return this;
        }

        /**
         * Add a byte array attachment.
         *
         * @param bytes the file bytes
         * @param fileName the file name
         * @return this builder
         */
        public EmailNotificationBuilder attachment(byte[] bytes, String fileName) {
            if (attachments == null) {
                attachments = new ArrayList<>();
            }
            attachments.add(EmailAttachment.builder()
                    .bytes(bytes)
                    .fileName(fileName)
                    .build());
            return this;
        }

        /**
         * Add an InputStream attachment.
         *
         * @param inputStream the input stream
         * @param fileName the file name
         * @param contentType the content type (MIME type)
         * @return this builder
         */
        public EmailNotificationBuilder attachment(InputStream inputStream, String fileName, String contentType) {
            if (attachments == null) {
                attachments = new ArrayList<>();
            }
            attachments.add(EmailAttachment.builder()
                    .inputStream(inputStream)
                    .fileName(fileName)
                    .contentType(contentType)
                    .build());
            return this;
        }

        /**
         * Add a MultipartFile attachment (from Spring file upload).
         * This is convenient when handling file uploads in REST controllers.
         *
         * @param multipartFile the multipart file from request
         * @return this builder
         * @throws IllegalArgumentException if multipartFile is null or empty
         */
        public EmailNotificationBuilder attachment(MultipartFile multipartFile) {
            if (multipartFile == null || multipartFile.isEmpty()) {
                throw new IllegalArgumentException("MultipartFile cannot be null or empty");
            }
            
            if (attachments == null) {
                attachments = new ArrayList<>();
            }
            
            try {
                String contentType = multipartFile.getContentType();
                String fileName = multipartFile.getOriginalFilename();
                
                // Fix content type if it's "multipart/form-data" (HTTP form type, not file type)
                if (contentType != null && contentType.equals("multipart/form-data")) {
                    // Try to detect from file extension
                    if (fileName != null) {
                        String fileNameLower = fileName.toLowerCase();
                        if (fileNameLower.endsWith(".pdf")) {
                            contentType = "application/pdf";
                        } else if (fileNameLower.endsWith(".jpg") || fileNameLower.endsWith(".jpeg")) {
                            contentType = "image/jpeg";
                        } else if (fileNameLower.endsWith(".png")) {
                            contentType = "image/png";
                        } else if (fileNameLower.endsWith(".gif")) {
                            contentType = "image/gif";
                        } else {
                            contentType = "application/octet-stream";
                        }
                    } else {
                        contentType = "application/octet-stream";
                    }
                }
                
                attachments.add(EmailAttachment.builder()
                        .bytes(multipartFile.getBytes())
                        .fileName(fileName)
                        .contentType(contentType)
                        .build());
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to read MultipartFile: " + e.getMessage(), e);
            }
            
            return this;
        }

        /**
         * Add an inline image from a file.
         *
         * @param file the image file
         * @param contentId the content ID (e.g., "logo" for cid:logo)
         * @return this builder
         */
        public EmailNotificationBuilder inlineImage(File file, String contentId) {
            if (attachments == null) {
                attachments = new ArrayList<>();
            }
            attachments.add(EmailAttachment.builder()
                    .file(file)
                    .isInline(true)
                    .contentId(contentId)
                    .build());
            return this;
        }

        /**
         * Add an inline image from byte array.
         *
         * @param bytes the image bytes
         * @param fileName the file name
         * @param contentId the content ID (e.g., "logo" for cid:logo)
         * @return this builder
         */
        public EmailNotificationBuilder inlineImage(byte[] bytes, String fileName, String contentId) {
            if (attachments == null) {
                attachments = new ArrayList<>();
            }
            attachments.add(EmailAttachment.builder()
                    .bytes(bytes)
                    .fileName(fileName)
                    .isInline(true)
                    .contentId(contentId)
                    .build());
            return this;
        }

        /**
         * Add an inline image from InputStream.
         *
         * @param inputStream the image input stream
         * @param fileName the file name
         * @param contentType the content type (MIME type, e.g., "image/png")
         * @param contentId the content ID (e.g., "logo" for cid:logo)
         * @return this builder
         */
        public EmailNotificationBuilder inlineImage(InputStream inputStream, String fileName, 
                                                    String contentType, String contentId) {
            if (attachments == null) {
                attachments = new ArrayList<>();
            }
            attachments.add(EmailAttachment.builder()
                    .inputStream(inputStream)
                    .fileName(fileName)
                    .contentType(contentType)
                    .isInline(true)
                    .contentId(contentId)
                    .build());
            return this;
        }

        /**
         * Add an inline image from MultipartFile (from Spring file upload).
         * This is convenient when handling image uploads in REST controllers.
         *
         * @param multipartFile the multipart file from request
         * @param contentId the content ID (e.g., "logo" for cid:logo)
         * @return this builder
         * @throws IllegalArgumentException if multipartFile is null or empty
         */
        public EmailNotificationBuilder inlineImage(MultipartFile multipartFile, String contentId) {
            if (multipartFile == null || multipartFile.isEmpty()) {
                throw new IllegalArgumentException("MultipartFile cannot be null or empty");
            }
            
            if (attachments == null) {
                attachments = new ArrayList<>();
            }
            
            try {
                String contentType = multipartFile.getContentType();
                String fileName = multipartFile.getOriginalFilename();
                
                // Fix content type if it's "multipart/form-data" (HTTP form type, not file type)
                if (contentType != null && contentType.equals("multipart/form-data")) {
                    // Try to detect from file extension
                    if (fileName != null) {
                        String fileNameLower = fileName.toLowerCase();
                        if (fileNameLower.endsWith(".jpg") || fileNameLower.endsWith(".jpeg")) {
                            contentType = "image/jpeg";
                        } else if (fileNameLower.endsWith(".png")) {
                            contentType = "image/png";
                        } else if (fileNameLower.endsWith(".gif")) {
                            contentType = "image/gif";
                        } else {
                            contentType = "image/jpeg"; // Default for inline images
                        }
                    } else {
                        contentType = "image/jpeg"; // Default for inline images
                    }
                }
                
                attachments.add(EmailAttachment.builder()
                        .bytes(multipartFile.getBytes())
                        .fileName(fileName)
                        .contentType(contentType)
                        .isInline(true)
                        .contentId(contentId)
                        .build());
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to read MultipartFile: " + e.getMessage(), e);
            }
            
            return this;
        }

        @Override
        protected NotificationRequest buildRequest() {
            return NotificationRequest.builder()
                    .notificationType(notificationType)
                    .to(to)
                    .cc(cc)
                    .bcc(bcc)
                    .subject(subject)
                    .body(body)
                    .templateName(templateName)
                    .templateVariables(templateVariables.isEmpty() ? null : templateVariables)
                    .from(from)
                    .replyTo(replyTo)
                    .attachments(attachments)
                    .build();
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
