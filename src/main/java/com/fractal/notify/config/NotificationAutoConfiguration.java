package com.fractal.notify.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * Auto-configuration for the notification engine.
 */
@Slf4j
@Configuration
@EnableAsync
@EnableConfigurationProperties(NotificationProperties.class)
@ConditionalOnProperty(prefix = "fractal.notify", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableJpaRepositories(basePackages = "com.fractal.notify.persistence.repository")
public class NotificationAutoConfiguration {

    @Bean
    public JavaMailSender javaMailSender(NotificationProperties properties) {
        if (!properties.getEmail().getProvider().equals("smtp")) {
            log.debug("SMTP not configured as email provider, skipping JavaMailSender configuration");
            return null;
        }

        NotificationProperties.SMTPConfig smtpConfig = properties.getEmail().getSmtp();
        if (smtpConfig.getHost() == null) {
            log.warn("SMTP host not configured");
            return null;
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(smtpConfig.getHost());
        mailSender.setPort(smtpConfig.getPort());
        mailSender.setUsername(smtpConfig.getUsername());
        mailSender.setPassword(smtpConfig.getPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");

        log.info("JavaMailSender configured for host: {}", smtpConfig.getHost());
        return mailSender;
    }

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor(NotificationProperties properties) {
        NotificationProperties.AsyncConfig asyncConfig = properties.getAsync();
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncConfig.getCorePoolSize());
        executor.setMaxPoolSize(asyncConfig.getMaxPoolSize());
        executor.setQueueCapacity(asyncConfig.getQueueCapacity());
        executor.setThreadNamePrefix("notification-");
        executor.initialize();
        
        log.info("Notification executor configured: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                asyncConfig.getCorePoolSize(), asyncConfig.getMaxPoolSize(), asyncConfig.getQueueCapacity());
        
        return executor;
    }
}
