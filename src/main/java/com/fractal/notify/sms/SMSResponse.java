package com.fractal.notify.sms;

import lombok.Builder;
import lombok.Data;

/**
 * Response model for SMS operations.
 */
@Data
@Builder
public class SMSResponse {
    private boolean success;
    private String messageId;
    private String errorMessage;
}
