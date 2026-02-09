package com.fractal.notify.storage;

import com.fractal.notify.config.NotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for creating storage providers based on configuration.
 * Follows the same pattern as EmailProviderFactory, SMSProviderFactory, etc.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorageProviderFactory {
    private final List<StorageProvider> storageProviders;
    private final NotificationProperties properties;

    /**
     * Get the configured storage provider.
     *
     * @return the storage provider
     */
    public StorageProvider getProvider() {
        String providerName = properties.getStorage().getProvider();
        log.debug("Getting storage provider: {}", providerName);

        Map<String, StorageProvider> providerMap = storageProviders.stream()
                .collect(Collectors.toMap(
                        StorageProvider::getProviderName,
                        Function.identity()
                ));

        StorageProvider provider = providerMap.get(providerName);
        if (provider == null) {
            log.warn("Storage provider '{}' not found, using default azure-blob", providerName);
            return providerMap.get("azure-blob");
        }

        return provider;
    }
}
