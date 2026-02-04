package com.fractal.notify.whatsapp.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Request model for WhatsApp notifications.
 */
@Data
@Builder
public class WhatsAppRequest {
    private String to;
    private String from;
    private String body;
}
