package com.fractal.notify.email;

import com.fractal.notify.email.dto.EmailRequest;

/**
 * Interface for email providers.
 * Different implementations can be created for SMTP, SendGrid, etc.
 */
public interface EmailProvider {
    /**
     * Send an email.
     *
     * @param request the email request
     * @return the email response
     */
    EmailResponse sendEmail(EmailRequest request);

    /**
     * Get the provider name.
     *
     * @return the provider name
     */
    String getProviderName();
}
