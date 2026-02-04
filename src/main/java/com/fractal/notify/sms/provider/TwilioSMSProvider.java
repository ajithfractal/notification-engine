package com.fractal.notify.sms.provider;

import com.fractal.notify.config.NotificationProperties;
import com.fractal.notify.sms.SMSProvider;
import com.fractal.notify.sms.SMSResponse;
import com.fractal.notify.sms.dto.SMSRequest;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Twilio implementation of SMSProvider.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TwilioSMSProvider implements SMSProvider {
    private final NotificationProperties properties;
    private static final String PROVIDER_NAME = "twilio";

    @PostConstruct
    public void init() {
        NotificationProperties.TwilioConfig twilioConfig = properties.getSms().getTwilio();
        if (twilioConfig != null && twilioConfig.getAccountSid() != null && twilioConfig.getAuthToken() != null) {
            Twilio.init(twilioConfig.getAccountSid(), twilioConfig.getAuthToken());
            log.info("Twilio initialized successfully");
        } else {
            log.warn("Twilio credentials not configured");
        }
    }

    @Override
    public SMSResponse sendSMS(SMSRequest request) {
        try {
            NotificationProperties.TwilioConfig twilioConfig = properties.getSms().getTwilio();
            if (twilioConfig == null || twilioConfig.getAccountSid() == null) {
                throw new IllegalStateException("Twilio not configured");
            }

            String fromNumber = request.getFrom() != null ? request.getFrom() : twilioConfig.getFromNumber();
            if (fromNumber == null) {
                throw new IllegalStateException("Twilio from number not configured");
            }

            Message message = Message.creator(
                    new PhoneNumber(request.getTo()),
                    new PhoneNumber(fromNumber),
                    request.getBody()
            ).create();

            log.info("SMS sent successfully via Twilio to {}. Message SID: {}", request.getTo(), message.getSid());
            return SMSResponse.builder()
                    .success(true)
                    .messageId(message.getSid())
                    .build();
        } catch (Exception e) {
            log.error("Error sending SMS via Twilio", e);
            return SMSResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
