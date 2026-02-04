package com.fractal.notify.template;

import com.fractal.notify.core.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Service for rendering notification templates using Thymeleaf.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {
    private final TemplateEngine templateEngine;

    /**
     * Render a template with the given variables.
     * Supports both template content (provided by client) and template name (from resources).
     * If templateContent is provided, it takes precedence over templateName.
     *
     * @param notificationType the type of notification
     * @param templateName the name of the template (from resources, optional)
     * @param templateContent the template content (provided by client, optional)
     * @param variables the variables to use in template rendering
     * @return the rendered content
     */
    public String renderTemplate(NotificationType notificationType, String templateName, String templateContent, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            
            String rendered;
            
            // If templateContent is provided, use it directly (client-provided template)
            if (templateContent != null && !templateContent.trim().isEmpty()) {
                log.debug("Rendering client-provided template content");
                rendered = templateEngine.process(templateContent, context);
            } 
            // Otherwise, use templateName to load from resources
            else if (templateName != null && !templateName.trim().isEmpty()) {
                String templatePath = String.format("%s/%s", notificationType.name().toLowerCase(), templateName);
                log.debug("Rendering template from resources: {}", templatePath);
                rendered = templateEngine.process(templatePath, context);
            } else {
                throw new IllegalArgumentException("Either templateName or templateContent must be provided");
            }
            
            log.debug("Template rendered successfully");
            return rendered;
        } catch (Exception e) {
            log.error("Error rendering template: templateName={}, hasContent={}", templateName, templateContent != null, e);
            throw new RuntimeException("Failed to render template", e);
        }
    }
}
