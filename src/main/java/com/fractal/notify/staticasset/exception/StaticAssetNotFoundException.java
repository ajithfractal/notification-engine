package com.fractal.notify.staticasset.exception;

/**
 * Exception thrown when a static asset is not found or not active.
 */
public class StaticAssetNotFoundException extends RuntimeException {
    public StaticAssetNotFoundException(String assetName) {
        super("Static asset not found or not active: " + assetName);
    }
    
    public StaticAssetNotFoundException(String assetName, String reason) {
        super("Static asset not found: " + assetName + " - " + reason);
    }
}
