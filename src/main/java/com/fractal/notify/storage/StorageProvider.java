package com.fractal.notify.storage;

import java.io.InputStream;

/**
 * Interface for storage providers (Azure Blob, S3, etc.).
 * Follows the same pattern as EmailProvider, SMSProvider, etc.
 */
public interface StorageProvider {
    /**
     * Upload a file to storage.
     *
     * @param inputStream the file input stream
     * @param fileName the file name
     * @param contentType the content type (MIME type)
     * @return the storage path/URL where the file was uploaded
     * @throws StorageException if upload fails
     */
    String upload(InputStream inputStream, String fileName, String contentType) throws StorageException;

    /**
     * Download a file from storage.
     *
     * @param storagePath the storage path/URL returned from upload()
     * @return the file input stream
     * @throws StorageException if download fails
     */
    InputStream download(String storagePath) throws StorageException;

    /**
     * Delete a file from storage.
     *
     * @param storagePath the storage path/URL to delete
     * @throws StorageException if deletion fails
     */
    void delete(String storagePath) throws StorageException;

    /**
     * Get the provider name (e.g., "azure-blob", "s3").
     *
     * @return the provider name
     */
    String getProviderName();
}
