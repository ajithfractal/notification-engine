package com.fractal.notify.whatsapp.provider;

import org.springframework.stereotype.Component;

import com.fractal.notify.config.NotificationProperties;
import com.fractal.notify.whatsapp.WhatsAppProvider;
import com.fractal.notify.whatsapp.WhatsAppResponse;
import com.fractal.notify.whatsapp.dto.WhatsAppRequest;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Twilio implementation of WhatsAppProvider.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TwilioWhatsAppProvider implements WhatsAppProvider {
    
	private static final String PROVIDER_NAME = "twilio";
	private final NotificationProperties properties;
	
    @PostConstruct
    public void init() {
        NotificationProperties.TwilioWhatsAppConfig twilioConfig = properties.getWhatsapp().getTwilio();
        if (twilioConfig != null && twilioConfig.getAccountSid() != null && twilioConfig.getAuthToken() != null) {
            try {
                Twilio.init(twilioConfig.getAccountSid(), twilioConfig.getAuthToken());
                log.info("Twilio WhatsApp initialized successfully");
            } catch (Exception e) {
                // Already initialized (possibly by SMS provider), that's fine
                log.debug("Twilio already initialized: {}", e.getMessage());
            }
        } else {
            log.warn("Twilio WhatsApp credentials not configured");
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

	@Override
	public WhatsAppResponse sendWhatsApp(WhatsAppRequest request) {
		try {
            NotificationProperties.TwilioWhatsAppConfig twilioConfig = properties.getWhatsapp().getTwilio();
            if (twilioConfig == null || twilioConfig.getAccountSid() == null) {
                throw new IllegalStateException("Twilio WhatsApp not configured. Please set fractal.notify.whatsapp.twilio.account-sid and auth-token");
            }

            // Get WhatsApp from number from WhatsApp Twilio config
            String whatsappFromNumber = twilioConfig.getWhatsappFromNumber();
            if (whatsappFromNumber == null || whatsappFromNumber.trim().isEmpty()) {
                throw new IllegalStateException("WhatsApp from number not configured. Use format: whatsapp:+1234567890. Set fractal.notify.whatsapp.twilio.whatsapp-from-number");
            }

            // Ensure WhatsApp format (whatsapp:+1234567890)
            String fromNumber = whatsappFromNumber.startsWith("whatsapp:") 
                ? whatsappFromNumber 
                : "whatsapp:" + whatsappFromNumber;

            // Ensure recipient is in WhatsApp format
            String toNumber = request.getTo();
            if (!toNumber.startsWith("whatsapp:")) {
                toNumber = "whatsapp:" + toNumber;
            }

            // Send WhatsApp message via Twilio
            Message message;
            if (request.getContentSid() != null && !request.getContentSid().trim().isEmpty()) {
                // Use Twilio Content Template (approved WhatsApp template)
                String contentSid = request.getContentSid().trim();
                
                // Validate Content Template SID format (should start with 'HX')
                if (!contentSid.startsWith("HX")) {
                    String errorMsg = String.format(
                        "Invalid Content Template SID: '%s'. Content Template SIDs must start with 'HX'. " +
                        "You may have entered an Account SID (starts with 'AC') instead. " +
                        "Please get the correct Content Template SID from Twilio Console → Content → Content Templates.",
                        contentSid
                    );
                    log.error(errorMsg);
                    throw new IllegalArgumentException(errorMsg);
                }
                
                log.debug("Sending WhatsApp message using Content Template: contentSid={}", contentSid);
                
                // For Content Templates, we need to use a different approach
                // Twilio requires either body OR contentSid, not both
                // Use a minimal placeholder body, then override with contentSid
                MessageCreator messageCreator = Message.creator(
                        new PhoneNumber(toNumber),
                        new PhoneNumber(fromNumber),
                        " "  // Use space instead of empty string (Twilio may reject empty)
                );
                
                // Set content SID (this will override the body)
                messageCreator.setContentSid(contentSid);
                
                // Add content variables if provided
                // Twilio Content Templates expect variables as a JSON string
                if (request.getContentVariables() != null && !request.getContentVariables().isEmpty()) {
                    try {
                        // Convert Map to JSON string manually
                        StringBuilder jsonBuilder = new StringBuilder("{");
                        boolean first = true;
                        for (Map.Entry<String, String> entry : request.getContentVariables().entrySet()) {
                            if (!first) {
                                jsonBuilder.append(",");
                            }
                            jsonBuilder.append("\"").append(escapeJson(entry.getKey())).append("\":");
                            jsonBuilder.append("\"").append(escapeJson(entry.getValue())).append("\"");
                            first = false;
                        }
                        jsonBuilder.append("}");
                        String contentVariablesJson = jsonBuilder.toString();
                        messageCreator.setContentVariables(contentVariablesJson);
                        log.debug("Content variables set: {}", contentVariablesJson);
                    } catch (Exception e) {
                        log.warn("Failed to serialize content variables to JSON, continuing without variables: {}", e.getMessage());
                    }
                }
                
                try {
                    message = messageCreator.create();
                } catch (com.twilio.exception.ApiException e) {
                    log.error("Twilio API error when sending Content Template. ContentSid: {}, Error: {}, Code: {}", 
                            contentSid, e.getMessage(), e.getCode());
                    throw new IllegalStateException("Failed to send WhatsApp Content Template: " + e.getMessage() + 
                            ". Please verify the Content Template SID is correct and approved in your Twilio account.", e);
                }
            } else {
                // Use regular body message
                if (request.getBody() == null || request.getBody().trim().isEmpty()) {
                    throw new IllegalStateException("Either body or contentSid must be provided for WhatsApp message");
                }
                message = Message.creator(
                        new PhoneNumber(toNumber),
                        new PhoneNumber(fromNumber),
                        request.getBody()
                ).create();
            }

            log.info("WhatsApp message sent successfully via Twilio to {}. Message SID: {}", 
                    request.getTo(), message.getSid());
            return WhatsAppResponse.builder()
                    .success(true)
                    .messageId(message.getSid())
                    .build();
        } catch (Exception e) {
            log.error("Error sending WhatsApp message via Twilio", e);
            return WhatsAppResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
	}
	
	/**
	 * Escape special characters in JSON string values.
	 */
	private String escapeJson(String value) {
	    if (value == null) {
	        return "";
	    }
	    return value.replace("\\", "\\\\")
	                .replace("\"", "\\\"")
	                .replace("\n", "\\n")
	                .replace("\r", "\\r")
	                .replace("\t", "\\t");
	}
}
