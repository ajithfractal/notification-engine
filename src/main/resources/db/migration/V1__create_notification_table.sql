-- Create notifications table for persisting notification requests
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    notification_type VARCHAR(20) NOT NULL,
    recipient_to TEXT[] NOT NULL,
    recipient_cc TEXT[],
    recipient_bcc TEXT[],
    subject VARCHAR(500),
    body TEXT,
    template_name VARCHAR(200),
    template_content TEXT,
    template_variables JSONB,
    from_address VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider VARCHAR(50),
    message_id VARCHAR(255),
    error_message TEXT,
    retry_count INT DEFAULT 0,
    cost DECIMAL(10,4),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP WITH TIME ZONE
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_notification_status ON notifications(status);
CREATE INDEX IF NOT EXISTS idx_notification_created_at ON notifications(created_at);
CREATE INDEX IF NOT EXISTS idx_notification_type ON notifications(notification_type);
