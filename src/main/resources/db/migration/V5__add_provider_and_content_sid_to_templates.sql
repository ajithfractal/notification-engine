-- Add provider and content_sid columns to templates table for WhatsApp Content Templates support
ALTER TABLE templates 
ADD COLUMN IF NOT EXISTS provider VARCHAR(50),
ADD COLUMN IF NOT EXISTS content_sid VARCHAR(200);

-- Create index for provider lookup
CREATE INDEX IF NOT EXISTS idx_template_provider ON templates(provider);

-- Create index for content_sid lookup
CREATE INDEX IF NOT EXISTS idx_template_content_sid ON templates(content_sid);

-- Add comment for documentation
COMMENT ON COLUMN templates.provider IS 'Provider name (e.g., TWILIO, AWS_SNS, etc.)';
COMMENT ON COLUMN templates.content_sid IS 'Twilio Content Template SID for WhatsApp approved templates (e.g., HX1234567890abcdef)';
