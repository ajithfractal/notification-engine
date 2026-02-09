package com.fractal.notify.email.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Request model for email notifications.
 */
@Getter
@Setter
@Builder
public class EmailRequest {
    private List<String> to;
    private List<String> cc;
    private List<String> bcc;
    private String from;
    private String replyTo;
    private String subject;
    private String body;
    private List<EmailAttachment> attachments;
}
