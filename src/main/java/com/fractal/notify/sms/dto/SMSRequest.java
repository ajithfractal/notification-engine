package com.fractal.notify.sms.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Request model for SMS notifications.
 */
@Getter
@Setter
@Builder
public class SMSRequest {
    private String to;
    private String from;
    private String body;
}
