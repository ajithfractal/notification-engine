package com.fractal.notify.sms.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Request model for SMS notifications.
 */
@Data
@Builder
public class SMSRequest {
    private String to;
    private String from;
    private String body;
}
