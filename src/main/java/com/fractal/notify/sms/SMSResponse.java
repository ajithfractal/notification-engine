package com.fractal.notify.sms;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Response model for SMS operations.
 */
@Getter
@Setter
@Builder
public class SMSResponse {
    private boolean success;
    private String messageId;
    private String errorMessage;
}
