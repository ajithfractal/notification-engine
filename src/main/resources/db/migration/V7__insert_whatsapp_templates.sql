-- Insert example WhatsApp templates for Twilio Content Templates
-- Replace the content_sid values with your actual Twilio Content Template SIDs

-- Welcome WhatsApp Template
-- Template variables: {{1}} = Company Name, {{2}} = User Name
INSERT INTO public.templates (
    name,
    notification_type,
    provider,
    content_sid,
    content,
    is_active,
    created_at,
    updated_at
)
VALUES (
    'WELCOME_WHATSAPP',
    'WHATSAPP',
    'TWILIO',
    'HX1234567890abcdef',  -- Replace with your actual Twilio Content Template SID (starts with HX)
    '👋 *Welcome to {{1}}!*

Hello {{2}},

We are delighted to welcome you to *{{1}}*. Your account has been successfully created, and you are now part of our platform. 🎉

If you need any assistance or have questions, our support team is always happy to help.

Kind regards,  
*{{1}} Team*',  -- Template content for reference
    true,
    now(),
    now()
)
ON CONFLICT (name, notification_type) DO NOTHING;

-- Order Confirmation WhatsApp Template (Example)
INSERT INTO public.templates (
    name,
    notification_type,
    provider,
    content_sid,
    content,
    is_active,
    created_at,
    updated_at
)
VALUES (
    'ORDER_CONFIRMATION_WHATSAPP',
    'WHATSAPP',
    'TWILIO',
    'HXae46e99cf2f96de49dbbc3aeb792ab2c',  -- Replace with your actual Twilio Content Template SID
    'Order confirmation template',  -- Description/fallback
    true,
    now(),
    now()
)
ON CONFLICT (name, notification_type) DO NOTHING;
