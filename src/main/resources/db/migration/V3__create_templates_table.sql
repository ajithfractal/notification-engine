-- Create templates table for storing notification templates
CREATE TABLE IF NOT EXISTS templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    notification_type VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(name, notification_type)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_template_name_type ON templates(name, notification_type);
CREATE INDEX IF NOT EXISTS idx_template_active ON templates(is_active);
