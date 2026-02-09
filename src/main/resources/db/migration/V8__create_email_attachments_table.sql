-- Create email_attachments table for storing attachment metadata
CREATE TABLE IF NOT EXISTS email_attachments (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    content_type VARCHAR(255),
    file_size BIGINT,
    storage_provider VARCHAR(50) NOT NULL,
    storage_path TEXT NOT NULL,
    is_inline BOOLEAN DEFAULT FALSE,
    content_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE
);

-- Create index for efficient lookups by notification_id
CREATE INDEX IF NOT EXISTS idx_email_attachment_notification_id ON email_attachments(notification_id);
