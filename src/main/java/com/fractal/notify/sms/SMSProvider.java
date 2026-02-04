package com.fractal.notify.sms;

import com.fractal.notify.sms.dto.SMSRequest;

/**
 * Interface for SMS providers.
 * Different implementations can be created for Twilio, AWS SNS, etc.
 */
public interface SMSProvider {
    /**
     * Send an SMS.
     *
     * @param request the SMS request
     * @return the SMS response
     */
    SMSResponse sendSMS(SMSRequest request);

    /**
     * Get the provider name.
     *
     * @return the provider name
     */
    String getProviderName();
}
