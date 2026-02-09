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
    /**
     * Twilio Content Template SID for WhatsApp approved templates.
     * When provided, uses Twilio Content Templates API instead of body.
     */
    private String contentSid;
    /**
     * Variables for Content Template (when using contentSid).
     * Key-value pairs matching the template variables.
     */
    private java.util.Map<String, String> contentVariables;
}
