package com.fractal.notify.whatsapp;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Response model for WhatsApp operations.
 */
@Getter
@Setter
@Builder
public class WhatsAppResponse {
    private boolean success;
    private String messageId;
    private String errorMessage;
}
