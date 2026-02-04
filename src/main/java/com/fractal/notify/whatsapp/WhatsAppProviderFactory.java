package com.fractal.notify.whatsapp;

import com.fractal.notify.config.NotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for creating WhatsApp providers based on configuration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WhatsAppProviderFactory {
    private final List<WhatsAppProvider> whatsAppProviders;
    private final NotificationProperties properties;

    /**
     * Get the configured WhatsApp provider.
     *
     * @return the WhatsApp provider
     */
    public WhatsAppProvider getProvider() {
        String providerName = properties.getWhatsapp().getProvider();
        log.debug("Getting WhatsApp provider: {}", providerName);

        Map<String, WhatsAppProvider> providerMap = whatsAppProviders.stream()
                .collect(Collectors.toMap(
                        WhatsAppProvider::getProviderName,
                        Function.identity()
                ));

        WhatsAppProvider provider = providerMap.get(providerName);
        if (provider == null) {
            log.warn("WhatsApp provider '{}' not found, using default", providerName);
            return providerMap.values().stream().findFirst().orElse(null);
        }

        return provider;
    }
}
