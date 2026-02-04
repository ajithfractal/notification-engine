package com.fractal.notify.email;

import lombok.Builder;
import lombok.Data;

/**
 * Response model for email operations.
 */
@Data
@Builder
public class EmailResponse {
    private boolean success;
    private String messageId;
    private String errorMessage;
}
