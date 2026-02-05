package com.fractal.notify.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;

/**
 * Auto-configuration for persistence layer.
 * Only enabled when fractal.notify.persistence.enabled=true
 */
@Slf4j
@Configuration
@EntityScan(basePackages = "com.fractal.notify.persistence.entity")
@EnableJpaRepositories(basePackages = "com.fractal.notify.persistence.repository")
@ConditionalOnProperty(prefix = "fractal.notify.persistence", name = "enabled", havingValue = "true", matchIfMissing = false)
public class PersistenceAutoConfiguration {
    
    public PersistenceAutoConfiguration() {
        log.info("Persistence auto-configuration enabled for notification engine");
    }

    /**
     * Configure DataSource properties from Spring Boot standard configuration.
     * This allows using spring.datasource.* properties.
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * Create DataSource bean.
     * Priority:
     * 1. If Spring Boot's standard datasource (spring.datasource.*) is configured, use it
     * 2. Otherwise, use library's custom datasource (fractal.notify.persistence.datasource.*)
     * 3. If client provides their own DataSource bean, use that (via @ConditionalOnMissingBean)
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource dataSource(NotificationProperties notificationProperties, DataSourceProperties dataSourceProperties) {
        // Check if Spring Boot standard datasource is configured
        if (dataSourceProperties.getUrl() != null && !dataSourceProperties.getUrl().isEmpty()) {
            log.info("Using Spring Boot standard datasource configuration (spring.datasource.*)");
            log.info("Database URL: {}", maskUrl(dataSourceProperties.getUrl()));
            return dataSourceProperties.initializeDataSourceBuilder()
                    .type(HikariDataSource.class)
                    .build();
        }

        // Otherwise, use library's custom datasource configuration
        NotificationProperties.DataSourceConfig customConfig = notificationProperties.getPersistence().getDatasource();
        if (customConfig.getUrl() == null || customConfig.getUrl().isEmpty()) {
            throw new IllegalStateException(
                    "DataSource URL not configured. Please configure one of the following:\n" +
                    "  1. Spring Boot standard: spring.datasource.url\n" +
                    "  2. Library custom: fractal.notify.persistence.datasource.url\n" +
                    "  3. Provide your own DataSource bean"
            );
        }

        log.info("Using library custom datasource configuration (fractal.notify.persistence.datasource.*)");
        log.info("Database URL: {}", maskUrl(customConfig.getUrl()));

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(customConfig.getUrl());
        dataSource.setUsername(customConfig.getUsername());
        dataSource.setPassword(customConfig.getPassword());
        if (customConfig.getDriverClassName() != null && !customConfig.getDriverClassName().isEmpty()) {
            dataSource.setDriverClassName(customConfig.getDriverClassName());
        } else {
            // Default to PostgreSQL if not specified
            dataSource.setDriverClassName("org.postgresql.Driver");
        }
        
        // Set reasonable defaults for connection pool
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(2);
        dataSource.setConnectionTimeout(30000);
        dataSource.setIdleTimeout(600000);
        dataSource.setMaxLifetime(1800000);

        return dataSource;
    }

    private String maskUrl(String url) {
        if (url == null) return "null";
        // Mask password in URL if present
        return url.replaceAll("password=[^;]+", "password=***");
    }
}
