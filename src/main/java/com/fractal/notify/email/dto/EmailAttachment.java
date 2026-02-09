package com.fractal.notify.email.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.InputStream;

/**
 * DTO for email attachments.
 * Supports multiple input types: File, byte[], and InputStream.
 */
@Getter
@Setter
@Builder
public class EmailAttachment {
    /**
     * File object (if attachment is from a File).
     */
    private File file;

    /**
     * Byte array (if attachment is from byte[]).
     */
    private byte[] bytes;

    /**
     * Input stream (if attachment is from InputStream).
     */
    private InputStream inputStream;

    /**
     * File name (required for byte[] and InputStream).
     */
    private String fileName;

    /**
     * Content type (MIME type, e.g., "application/pdf", "image/png").
     */
    private String contentType;

    /**
     * Whether this is an inline image (embedded in email body).
     */
    @Builder.Default
    private boolean isInline = false;

    /**
     * Content ID for inline images (e.g., "logo" for cid:logo).
     * Required if isInline is true.
     */
    private String contentId;

    /**
     * Get the file name from the attachment.
     * For File objects, uses the file name.
     * For byte[] and InputStream, uses the provided fileName.
     */
    public String getFileName() {
        if (file != null) {
            return file.getName();
        }
        return fileName;
    }

    /**
     * Get the content type from the attachment.
     * For File objects, attempts to detect from file extension.
     * For byte[] and InputStream, uses the provided contentType.
     */
    public String getContentType() {
        if (contentType != null && !contentType.isEmpty()) {
            return contentType;
        }
        
        if (file != null) {
            // Try to detect from file extension
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".pdf")) {
                return "application/pdf";
            } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                return "image/jpeg";
            } else if (fileName.endsWith(".png")) {
                return "image/png";
            } else if (fileName.endsWith(".gif")) {
                return "image/gif";
            } else if (fileName.endsWith(".txt")) {
                return "text/plain";
            } else if (fileName.endsWith(".html")) {
                return "text/html";
            } else if (fileName.endsWith(".doc")) {
                return "application/msword";
            } else if (fileName.endsWith(".docx")) {
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            } else if (fileName.endsWith(".xls")) {
                return "application/vnd.ms-excel";
            } else if (fileName.endsWith(".xlsx")) {
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            }
        }
        
        // Default content type
        return "application/octet-stream";
    }
}
