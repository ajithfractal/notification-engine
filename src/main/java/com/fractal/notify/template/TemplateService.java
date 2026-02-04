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
     *
     * @param notificationType the type of notification
     * @param templateName the name of the template
     * @param variables the variables to use in template rendering
     * @return the rendered content
     */
    public String renderTemplate(NotificationType notificationType, String templateName, Map<String, Object> variables) {
        try {
            String templatePath = String.format("%s/%s", notificationType.name().toLowerCase(), templateName);
            Context context = new Context();
            context.setVariables(variables);
            
            String rendered = templateEngine.process(templatePath, context);
            log.debug("Template {} rendered successfully", templatePath);
            return rendered;
        } catch (Exception e) {
            log.error("Error rendering template: {}", templateName, e);
            throw new RuntimeException("Failed to render template: " + templateName, e);
        }
    }
}
