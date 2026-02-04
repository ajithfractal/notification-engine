package com.fractal.notify.whatsapp.provider;

import com.fractal.notify.whatsapp.WhatsAppProvider;
import com.fractal.notify.whatsapp.WhatsAppResponse;
import com.fractal.notify.whatsapp.dto.WhatsAppRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Default stub implementation of WhatsAppProvider.
 * This is a placeholder for future WhatsApp provider implementations.
 */
@Slf4j
@Component
public class DefaultWhatsAppProvider implements WhatsAppProvider {
    private static final String PROVIDER_NAME = "default";

    @Override
    public WhatsAppResponse sendWhatsApp(WhatsAppRequest request) {
        log.warn("WhatsApp provider not yet implemented. Request to: {}", request.getTo());
        return WhatsAppResponse.builder()
                .success(false)
                .errorMessage("WhatsApp provider not yet implemented")
                .build();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
