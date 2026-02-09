package com.fractal.notify.template;

import com.fractal.notify.core.NotificationType;
import com.fractal.notify.persistence.entity.TemplateEntity;
import com.fractal.notify.persistence.repository.TemplateRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Map;

/**
 * Service for rendering notification templates using Thymeleaf.
 * Templates are loaded from database only.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {
    private final TemplateRepository templateRepository;
    private TemplateEngine stringTemplateEngine;

    @PostConstruct
    public void init() {
        // Create a separate TemplateEngine for processing string templates from database
        stringTemplateEngine = new TemplateEngine();
        
        // Configure StringTemplateResolver
        StringTemplateResolver templateResolver = new StringTemplateResolver();
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCacheable(false); // Don't cache string templates (they come from DB)
        stringTemplateEngine.setTemplateResolver(templateResolver);
        
        log.debug("StringTemplateEngine initialized for database templates with StandardDialect");
    }

    /**
     * Get template entity from database.
     * Template is loaded from database only.
     *
     * @param notificationType the type of notification
     * @param templateName the name of the template (required)
     * @return the template entity
     * @throws TemplateNotFoundException if template not found or not active
     */
    public TemplateEntity getTemplate(NotificationType notificationType, String templateName) {
        if (templateName == null || templateName.trim().isEmpty()) {
            throw new IllegalArgumentException("Template name is required");
        }

        return templateRepository
                .findByNameAndNotificationTypeAndIsActiveTrue(templateName, notificationType)
                .orElseThrow(() -> {
                    log.error("Active template not found: name={}, type={}", templateName, notificationType);
                    return new TemplateNotFoundException(templateName, notificationType);
                });
    }

    /**
     * Render a template with the given variables.
     * Template is loaded from database only.
     *
     * @param notificationType the type of notification
     * @param templateName the name of the template (required)
     * @param variables the variables to use in template rendering
     * @return the rendered content
     * @throws TemplateNotFoundException if template not found or not active
     */
    public String renderTemplate(NotificationType notificationType, String templateName, Map<String, Object> variables) {
        TemplateEntity template = getTemplate(notificationType, templateName);

        try {
            // Render template using StringTemplateResolver
            Context context = new Context();
            context.setVariables(variables);
            
            // Use stringTemplateEngine to process the template content directly from database
            String rendered = stringTemplateEngine.process(template.getContent(), context);
            log.debug("Template '{}' rendered successfully", templateName);
            
            return rendered;
        } catch (Exception e) {
            log.error("Error rendering template: templateName={}, type={}", templateName, notificationType, e);
            throw new RuntimeException("Failed to render template: " + e.getMessage(), e);
        }
    }
}
