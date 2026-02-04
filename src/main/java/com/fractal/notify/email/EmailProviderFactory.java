package com.fractal.notify.email;

import com.fractal.notify.config.NotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for creating email providers based on configuration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailProviderFactory {
    private final List<EmailProvider> emailProviders;
    private final NotificationProperties properties;

    /**
     * Get the configured email provider.
     *
     * @return the email provider
     */
    public EmailProvider getProvider() {
        String providerName = properties.getEmail().getProvider();
        log.debug("Getting email provider: {}", providerName);

        Map<String, EmailProvider> providerMap = emailProviders.stream()
                .collect(Collectors.toMap(
                        EmailProvider::getProviderName,
                        Function.identity()
                ));

        EmailProvider provider = providerMap.get(providerName);
        if (provider == null) {
            log.warn("Email provider '{}' not found, using default SMTP", providerName);
            return providerMap.get("smtp");
        }

        return provider;
    }
}
