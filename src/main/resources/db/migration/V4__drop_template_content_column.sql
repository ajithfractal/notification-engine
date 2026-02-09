-- Drop template_content column from notifications table
-- This column is no longer used as templates are now stored in the templates table
ALTER TABLE notifications DROP COLUMN IF EXISTS template_content;
