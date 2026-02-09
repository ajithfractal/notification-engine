-- Update status check constraint to include PROCESSING status
-- Drop existing constraint if it exists
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_status_check;

-- Add new constraint with all valid status values
ALTER TABLE notifications ADD CONSTRAINT notifications_status_check 
    CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'RETRYING'));
