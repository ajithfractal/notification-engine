package com.fractal.notify.whatsapp;

import com.fractal.notify.whatsapp.dto.WhatsAppRequest;

/**
 * Interface for WhatsApp providers.
 * Future implementations can be created for different WhatsApp API providers.
 */
public interface WhatsAppProvider {
    /**
     * Send a WhatsApp message.
     *
     * @param request the WhatsApp request
     * @return the WhatsApp response
     */
    WhatsAppResponse sendWhatsApp(WhatsAppRequest request);

    /**
     * Get the provider name.
     *
     * @return the provider name
     */
    String getProviderName();
}
