package com.fractal.notify.email.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Request model for email notifications.
 */
@Data
@Builder
public class EmailRequest {
    private List<String> to;
    private List<String> cc;
    private List<String> bcc;
    private String from;
    private String subject;
    private String body;
}
