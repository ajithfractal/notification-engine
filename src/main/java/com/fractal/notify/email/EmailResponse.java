package com.fractal.notify.email;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Response model for email operations.
 */
@Getter
@Setter
@Builder
public class EmailResponse {
    private boolean success;
    private String messageId;
    private String errorMessage;
}
