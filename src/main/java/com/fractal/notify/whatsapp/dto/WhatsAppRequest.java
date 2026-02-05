package com.fractal.notify.whatsapp.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Request model for WhatsApp notifications.
 */
@Getter
@Setter
@Builder
public class WhatsAppRequest {
    private String to;
    private String from;
    private String body;
}
