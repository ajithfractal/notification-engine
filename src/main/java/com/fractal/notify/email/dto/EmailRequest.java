package com.fractal.notify.email.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Request model for email notifications.
 */
@Data
@Builder
public class EmailRequest {
    private String to;
    private String from;
    private String subject;
    private String body;
}
