package com.fractal.notify.staticasset;

import com.fractal.notify.persistence.entity.StaticAssetEntity;
import com.fractal.notify.persistence.repository.StaticAssetRepository;
import com.fractal.notify.staticasset.exception.StaticAssetNotFoundException;
import com.fractal.notify.storage.StorageException;
import com.fractal.notify.storage.StorageProvider;
import com.fractal.notify.storage.StorageProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for managing static assets (logos, headers, footers) used in email templates.
 * Replaces cid: references in HTML with direct URLs to storage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaticAssetService {
    private final StaticAssetRepository staticAssetRepository;
    private final StorageProviderFactory storageProviderFactory;
    
    // Pattern to find cid: references (e.g., <img src='cid:logo'> or <img src="cid:logo">)
    private static final Pattern CID_PATTERN = Pattern.compile(
        "(src=['\"]?)cid:([a-zA-Z0-9_-]+)(['\"]?)", 
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Replace cid: references in HTML with direct URLs to static assets.
     * Example: <img src='cid:logo'> becomes <img src='https://storage.../logo.png'>
     *
     * @param htmlBody the HTML body with cid: references
     * @return HTML with URLs replaced
     */
    public String replaceStaticAssetReferences(String htmlBody) {
        if (htmlBody == null || htmlBody.isEmpty()) {
            return htmlBody;
        }
        
        Matcher matcher = CID_PATTERN.matcher(htmlBody);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String contentId = matcher.group(2); // The CID value (e.g., "logo")
            String quoteBefore = matcher.group(1); // Quote before cid:
            String quoteAfter = matcher.group(3); // Quote after cid:
            
            // Find static asset by content_id (or name)
            String url = getAssetUrl(contentId);
            
            if (url != null) {
                // Replace cid:logo with actual URL
                String replacement = quoteBefore + url + quoteAfter;
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                log.debug("Replaced cid:{} with URL: {}", contentId, url);
            } else {
                // Keep original if asset not found (don't break the HTML)
                log.warn("Static asset not found for cid: {}, keeping original reference", contentId);
                matcher.appendReplacement(result, matcher.group(0));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    /**
     * Get public URL for a static asset.
     * First tries to find by content_id, then by name.
     *
     * @param identifier content ID or asset name
     * @return public URL or null if not found
     */
    private String getAssetUrl(String identifier) {
        StaticAssetEntity asset = null;
        
        // Try to find by content_id first (if it's a CID reference)
        asset = staticAssetRepository
                .findByContentIdAndIsActiveTrue(identifier)
                .orElse(null);
        
        // If not found, try by name
        if (asset == null) {
            asset = staticAssetRepository
                    .findByNameAndIsActiveTrue(identifier)
                    .orElse(null);
        }
        
        if (asset == null) {
            log.debug("Static asset not found for identifier: {}", identifier);
            return null;
        }
        
        // Use public_url if available, otherwise generate from storage path
        if (asset.getPublicUrl() != null && !asset.getPublicUrl().isEmpty()) {
            log.debug("Using stored public URL for asset: {}", asset.getName());
            return asset.getPublicUrl();
        }
        
        // Generate URL from storage path
        try {
            StorageProvider storageProvider = storageProviderFactory.getProvider();
            String url = storageProvider.getPublicUrl(asset.getStoragePath());
            log.debug("Generated public URL for asset: {} -> {}", asset.getName(), url);
            return url;
        } catch (StorageException e) {
            log.error("Failed to generate public URL for asset: {}", asset.getName(), e);
            return null;
        }
    }

    /**
     * Get static asset by name.
     *
     * @param name the asset name
     * @return the static asset entity
     * @throws StaticAssetNotFoundException if asset not found or not active
     */
    public StaticAssetEntity getAsset(String name) {
        return staticAssetRepository
                .findByNameAndIsActiveTrue(name)
                .orElseThrow(() -> {
                    log.error("Active static asset not found: name={}", name);
                    return new StaticAssetNotFoundException(name);
                });
    }

    /**
     * Get static asset by name (including inactive).
     *
     * @param name the asset name
     * @return the static asset entity
     * @throws StaticAssetNotFoundException if asset not found
     */
    public StaticAssetEntity getAssetIncludingInactive(String name) {
        return staticAssetRepository
                .findByName(name)
                .orElseThrow(() -> {
                    log.error("Static asset not found: name={}", name);
                    return new StaticAssetNotFoundException(name);
                });
    }

    /**
     * Get all active static assets.
     *
     * @return list of active static assets
     */
    public java.util.List<StaticAssetEntity> getAllActiveAssets() {
        return staticAssetRepository.findByIsActiveTrue();
    }
}
