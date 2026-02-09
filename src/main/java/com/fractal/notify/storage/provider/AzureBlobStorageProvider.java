package com.fractal.notify.storage.provider;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.fractal.notify.config.NotificationProperties;
import com.fractal.notify.storage.StorageException;
import com.fractal.notify.storage.StorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Azure Blob Storage implementation of StorageProvider.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AzureBlobStorageProvider implements StorageProvider {
    private final NotificationProperties properties;
    private static final String PROVIDER_NAME = "azure-blob";
    private BlobServiceClient blobServiceClient;
    private BlobContainerClient containerClient;

    @Override
    public String upload(InputStream inputStream, String fileName, String contentType) throws StorageException {
        try {
            initializeClient();
            
            // Generate unique blob name: timestamp-uuid-filename
            String blobName = generateBlobName(fileName);
            
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            
            // Upload the file
            blobClient.upload(inputStream, true);
            
            // Set content type
            BlobHttpHeaders headers = new BlobHttpHeaders();
            headers.setContentType(contentType);
            blobClient.setHttpHeaders(headers);
            
            String storagePath = blobClient.getBlobUrl();
            log.debug("File uploaded to Azure Blob Storage: {}", storagePath);
            
            return storagePath;
        } catch (Exception e) {
            log.error("Error uploading file to Azure Blob Storage", e);
            throw new StorageException("Failed to upload file to Azure Blob Storage: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String storagePath) throws StorageException {
        try {
            initializeClient();
            
            // Extract blob name from URL
            String blobName = extractBlobNameFromUrl(storagePath);
            
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            
            if (!blobClient.exists()) {
                throw new StorageException("Blob not found: " + blobName);
            }
            
            // Download blob to byte array, then return as InputStream
            byte[] data = blobClient.downloadContent().toBytes();
            log.debug("File downloaded from Azure Blob Storage: {}", blobName);
            
            return new ByteArrayInputStream(data);
        } catch (Exception e) {
            log.error("Error downloading file from Azure Blob Storage", e);
            throw new StorageException("Failed to download file from Azure Blob Storage: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String storagePath) throws StorageException {
        try {
            initializeClient();
            
            // Extract blob name from URL
            String blobName = extractBlobNameFromUrl(storagePath);
            
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            
            if (blobClient.exists()) {
                blobClient.delete();
                log.debug("File deleted from Azure Blob Storage: {}", blobName);
            } else {
                log.warn("Blob not found for deletion: {}", blobName);
            }
        } catch (Exception e) {
            log.error("Error deleting file from Azure Blob Storage", e);
            throw new StorageException("Failed to delete file from Azure Blob Storage: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public String getPublicUrl(String storagePath) throws StorageException {
        try {
            initializeClient();
            
            // Extract blob name from URL or use storagePath directly if it's already a blob name
            String blobName;
            if (storagePath.startsWith("http://") || storagePath.startsWith("https://")) {
                blobName = extractBlobNameFromUrl(storagePath);
            } else {
                // Assume it's already a blob name (relative path)
                blobName = storagePath;
            }
            
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            
            if (!blobClient.exists()) {
                throw new StorageException("Blob not found: " + blobName);
            }
            
            // Option 1: If container is public, return direct URL
            // return blobClient.getBlobUrl();
            
            // Option 2: Generate SAS URL (more secure, valid for 1 year)
            // This is better for private containers
            OffsetDateTime expiryTime = OffsetDateTime.now().plusYears(1);
            BlobSasPermission permission = new BlobSasPermission()
                    .setReadPermission(true);
            
            BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(
                    expiryTime, permission);
            
            String sasToken = blobClient.generateSas(sasValues);
            String publicUrl = blobClient.getBlobUrl() + "?" + sasToken;
            
            log.debug("Generated public URL for blob: {}", blobName);
            return publicUrl;
            
        } catch (Exception e) {
            log.error("Error generating public URL for storage path: {}", storagePath, e);
            throw new StorageException("Failed to generate public URL: " + e.getMessage(), e);
        }
    }

    private void initializeClient() {
        if (blobServiceClient == null) {
            String connectionString = properties.getStorage().getAzureBlob().getConnectionString();
            if (connectionString == null || connectionString.isEmpty()) {
                throw new StorageException("Azure Blob Storage connection string is not configured");
            }
            
            blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();
        }
        
        if (containerClient == null) {
            String containerName = properties.getStorage().getAzureBlob().getContainerName();
            if (containerName == null || containerName.isEmpty()) {
                throw new StorageException("Azure Blob Storage container name is not configured");
            }
            
            containerClient = blobServiceClient.getBlobContainerClient(containerName);
            
            // Create container if it doesn't exist
            if (!containerClient.exists()) {
                containerClient.create();
                log.info("Created Azure Blob Storage container: {}", containerName);
            }
        }
    }

    private String generateBlobName(String fileName) {
        // Format: timestamp-uuid-filename
        // Example: 20240206120000-550e8400-e29b-41d4-a716-446655440000-document.pdf
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String uuid = UUID.randomUUID().toString();
        return String.format("%s-%s-%s", timestamp, uuid, fileName);
    }

    private String extractBlobNameFromUrl(String url) {
        // Extract blob name from Azure Blob URL
        // URL format: https://account.blob.core.windows.net/container/blob-name
        try {
            String[] parts = url.split("/");
            // Find the container name index and get the blob name (everything after container)
            int containerIndex = -1;
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].contains(".blob.core.windows.net")) {
                    containerIndex = i + 1;
                    break;
                }
            }
            
            if (containerIndex > 0 && containerIndex < parts.length - 1) {
                // Reconstruct blob name (may contain slashes)
                StringBuilder blobName = new StringBuilder();
                for (int i = containerIndex + 1; i < parts.length; i++) {
                    if (blobName.length() > 0) {
                        blobName.append("/");
                    }
                    blobName.append(parts[i]);
                }
                return blobName.toString();
            }
            
            // Fallback: return last part of URL
            return parts[parts.length - 1];
        } catch (Exception e) {
            log.warn("Could not extract blob name from URL: {}. Using full URL as blob name.", url);
            return url;
        }
    }
}
