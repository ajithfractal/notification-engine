package com.fractal.notify.template;

import com.fractal.notify.core.NotificationType;

public class TemplateNotFoundException extends RuntimeException {
    private final String templateName;
    private final NotificationType notificationType;

    public TemplateNotFoundException(String templateName, NotificationType notificationType) {
        super(String.format("Active template not found: name='%s', type=%s", templateName, notificationType));
        this.templateName = templateName;
        this.notificationType = notificationType;
    }

    public String getTemplateName() {
        return templateName;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }
}
