-- Create static_assets table for storing reusable static assets (logos, headers, footers, etc.)
-- These are referenced in email templates via cid: references and replaced with direct URLs
CREATE TABLE IF NOT EXISTS static_assets (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    storage_path TEXT NOT NULL,
    public_url TEXT,
    file_name VARCHAR(500) NOT NULL,
    content_type VARCHAR(255),
    file_size BIGINT,
    storage_provider VARCHAR(50) NOT NULL DEFAULT 'azure-blob',
    content_id VARCHAR(255),
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
