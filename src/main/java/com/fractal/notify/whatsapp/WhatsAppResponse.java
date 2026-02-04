package com.fractal.notify.whatsapp;

import lombok.Builder;
import lombok.Data;

/**
 * Response model for WhatsApp operations.
 */
@Data
@Builder
public class WhatsAppResponse {
    private boolean success;
    private String messageId;
    private String errorMessage;
}
