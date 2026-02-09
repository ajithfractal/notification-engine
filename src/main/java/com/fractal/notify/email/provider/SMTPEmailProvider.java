package com.fractal.notify.email.provider;

import com.fractal.notify.config.NotificationProperties;
import com.fractal.notify.email.EmailProvider;
import com.fractal.notify.email.EmailResponse;
import com.fractal.notify.email.dto.EmailAttachment;
import com.fractal.notify.email.dto.EmailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * SMTP implementation of EmailProvider using Spring Mail.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SMTPEmailProvider implements EmailProvider {
    private final JavaMailSender mailSender;
    private final NotificationProperties properties;
    private static final String PROVIDER_NAME = "smtp";

    @Override
    public EmailResponse sendEmail(EmailRequest request) {
        if (mailSender == null) {
            log.error("JavaMailSender not configured. Please configure SMTP settings.");
            return EmailResponse.builder()
                    .success(false)
                    .errorMessage("SMTP not configured")
                    .build();
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set TO recipients (supports multiple)
            if (request.getTo() != null && !request.getTo().isEmpty()) {
                helper.setTo(request.getTo().toArray(new String[0]));
            } else {
                throw new IllegalArgumentException("At least one 'to' recipient is required");
            }
            
            // Set CC if provided
            if (request.getCc() != null && !request.getCc().isEmpty()) {
                helper.setCc(request.getCc().toArray(new String[0]));
            }
            
            // Set BCC if provided
            if (request.getBcc() != null && !request.getBcc().isEmpty()) {
                helper.setBcc(request.getBcc().toArray(new String[0]));
            }
            
            helper.setFrom(request.getFrom() != null ? request.getFrom() : getDefaultFrom());
            
            // Set REPLY-TO (from request or fallback to properties)
            String replyTo = request.getReplyTo() != null ? request.getReplyTo() : getDefaultReplyTo();
            if (replyTo != null && !replyTo.isEmpty()) {
                helper.setReplyTo(replyTo);
            }
            
            helper.setSubject(request.getSubject());
            helper.setText(request.getBody(), true); // true indicates HTML

            // Handle attachments
            if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
                log.info("Adding {} attachment(s) to email", request.getAttachments().size());
                addAttachments(helper, request.getAttachments());
                log.info("Successfully added all attachments to email");
            } else {
                log.debug("No attachments to add to email");
            }

            mailSender.send(message);

            log.info("Email sent successfully via SMTP to {}", request.getTo());
            return EmailResponse.builder()
                    .success(true)
                    .messageId("smtp-" + System.currentTimeMillis())
                    .build();
        } catch (Exception e) {
            log.error("Error sending email via SMTP", e);
            return EmailResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    private String getDefaultFrom() {
        if (properties != null && properties.getEmail().getSmtp().getFrom() != null) {
            return properties.getEmail().getSmtp().getFrom();
        }
        return "noreply@fractal.com";
    }

    private String getDefaultReplyTo() {
        if (properties != null && properties.getEmail().getSmtp().getReplyTo() != null) {
            return properties.getEmail().getSmtp().getReplyTo();
        }
        return null;
    }

    /**
     * Add attachments to the email message.
     *
     * @param helper the MimeMessageHelper
     * @param attachments the list of attachments
     */
    private void addAttachments(MimeMessageHelper helper, List<EmailAttachment> attachments) {
        for (EmailAttachment attachment : attachments) {
            try {
                String fileName = attachment.getFileName();
                String contentType = attachment.getContentType();

                log.debug("Adding attachment: {} (type: {}, inline: {})", 
                        fileName, contentType, attachment.isInline());

                if (attachment.isInline() && attachment.getContentId() != null) {
                    // Inline image
                    addInlineAttachment(helper, attachment, fileName, contentType);
                    log.debug("Added inline image: {} with contentId: {}", fileName, attachment.getContentId());
                } else {
                    // Regular attachment
                    addRegularAttachment(helper, attachment, fileName, contentType);
                    log.debug("Added regular attachment: {}", fileName);
                }
            } catch (Exception e) {
                log.error("Error adding attachment: {} - {}", attachment.getFileName(), e.getMessage(), e);
                // Continue with other attachments even if one fails
            }
        }
    }

    /**
     * Add a regular attachment (not inline).
     */
    private void addRegularAttachment(MimeMessageHelper helper, EmailAttachment attachment, 
                                       String fileName, String contentType) throws Exception {
        if (attachment.getFile() != null) {
            // File attachment
            FileSystemResource fileResource = new FileSystemResource(attachment.getFile());
            helper.addAttachment(fileName, fileResource, contentType);
            log.debug("Added file attachment: {} ({} bytes)", fileName, attachment.getFile().length());
        } else if (attachment.getBytes() != null) {
            // Byte array attachment
            byte[] bytes = attachment.getBytes();
            ByteArrayResource byteArrayResource = new ByteArrayResource(bytes);
            helper.addAttachment(fileName, byteArrayResource, contentType);
            log.debug("Added byte array attachment: {} ({} bytes)", fileName, bytes.length);
        } else if (attachment.getInputStream() != null) {
            // InputStream attachment
            InputStreamResource inputStreamResource = new InputStreamResource(attachment.getInputStream());
            helper.addAttachment(fileName, inputStreamResource, contentType);
            log.debug("Added input stream attachment: {}", fileName);
        } else {
            throw new IllegalArgumentException("Attachment must have file, bytes, or inputStream");
        }
    }

    /**
     * Add an inline image attachment.
     */
    private void addInlineAttachment(MimeMessageHelper helper, EmailAttachment attachment,
                                     String fileName, String contentType) throws Exception {
        String contentId = attachment.getContentId();
        
        if (attachment.getFile() != null) {
            // File inline image
            FileSystemResource fileResource = new FileSystemResource(attachment.getFile());
            helper.addInline(contentId, fileResource, contentType);
        } else if (attachment.getBytes() != null) {
            // Byte array inline image
            ByteArrayResource byteArrayResource = new ByteArrayResource(attachment.getBytes());
            helper.addInline(contentId, byteArrayResource, contentType);
        } else if (attachment.getInputStream() != null) {
            // InputStream inline image
            InputStreamResource inputStreamResource = new InputStreamResource(attachment.getInputStream());
            helper.addInline(contentId, inputStreamResource, contentType);
        }
    }
}
