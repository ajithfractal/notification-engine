package com.fractal.notify.sms;

import com.fractal.notify.config.NotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for creating SMS providers based on configuration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SMSProviderFactory {
    private final List<SMSProvider> smsProviders;
    private final NotificationProperties properties;

    /**
     * Get the configured SMS provider.
     *
     * @return the SMS provider
     */
    public SMSProvider getProvider() {
        String providerName = properties.getSms().getProvider();
        log.debug("Getting SMS provider: {}", providerName);

        Map<String, SMSProvider> providerMap = smsProviders.stream()
                .collect(Collectors.toMap(
                        SMSProvider::getProviderName,
                        Function.identity()
                ));

        SMSProvider provider = providerMap.get(providerName);
        if (provider == null) {
            log.warn("SMS provider '{}' not found, using default Twilio", providerName);
            return providerMap.get("twilio");
        }

        return provider;
    }
}
