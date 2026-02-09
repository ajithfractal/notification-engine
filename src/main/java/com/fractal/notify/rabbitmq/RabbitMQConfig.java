package com.fractal.notify.rabbitmq;

import com.fractal.notify.config.NotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for notification message queue.
 * This configuration is only active when fractal.notify.async.mode=rabbitmq
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "fractal.notify.async",
    name = "mode",
    havingValue = "rabbitmq"
)
public class RabbitMQConfig {
    private final NotificationProperties properties;

    /**
     * Configure RabbitMQ connection factory.
     */
    @Bean
    public ConnectionFactory connectionFactory() {
        NotificationProperties.RabbitMQConfig rabbitmqConfig = properties.getRabbitmq();
        
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(rabbitmqConfig.getHost() != null ? rabbitmqConfig.getHost() : "localhost");
        connectionFactory.setPort(rabbitmqConfig.getPort());
        connectionFactory.setUsername(rabbitmqConfig.getUsername() != null ? rabbitmqConfig.getUsername() : "guest");
        connectionFactory.setPassword(rabbitmqConfig.getPassword() != null ? rabbitmqConfig.getPassword() : "guest");
        connectionFactory.setVirtualHost(rabbitmqConfig.getVirtualHost() != null ? rabbitmqConfig.getVirtualHost() : "/");
        connectionFactory.setConnectionTimeout(rabbitmqConfig.getConnectionTimeout());
        connectionFactory.setRequestedHeartBeat(rabbitmqConfig.getRequestedHeartbeat());
        
        log.info("RabbitMQ connection configured: {}:{}/{}", 
                rabbitmqConfig.getHost(), 
                rabbitmqConfig.getPort(), 
                rabbitmqConfig.getVirtualHost());
        
        return connectionFactory;
    }

    /**
     * Configure JSON message converter for RabbitMQ messages.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configure RabbitTemplate with JSON converter.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        if (messageConverter != null) {
            template.setMessageConverter(messageConverter);
        }
        return template;
    }

    /**
     * Declare topic exchange for notifications.
     */
    @Bean
    public TopicExchange notificationExchange() {
        String exchangeName = properties.getRabbitmq().getExchange() != null 
                ? properties.getRabbitmq().getExchange() 
                : "fractal.notifications";
        log.info("Declaring RabbitMQ exchange: {}", exchangeName);
        return ExchangeBuilder.topicExchange(exchangeName)
                .durable(true)
                .build();
    }

    /**
     * Declare email notification queue.
     */
    @Bean
    public Queue emailQueue() {
        String queueName = properties.getRabbitmq().getQueue().getEmail();
        log.info("Declaring RabbitMQ queue: {}", queueName);
        return QueueBuilder.durable(queueName).build();
    }

    /**
     * Declare SMS notification queue.
     */
    @Bean
    public Queue smsQueue() {
        String queueName = properties.getRabbitmq().getQueue().getSms();
        log.info("Declaring RabbitMQ queue: {}", queueName);
        return QueueBuilder.durable(queueName).build();
    }

    /**
     * Declare WhatsApp notification queue.
     */
    @Bean
    public Queue whatsappQueue() {
        String queueName = properties.getRabbitmq().getQueue().getWhatsapp();
        log.info("Declaring RabbitMQ queue: {}", queueName);
        return QueueBuilder.durable(queueName).build();
    }

    /**
     * Bind email queue to exchange with routing key.
     */
    @Bean
    public Binding emailBinding(TopicExchange notificationExchange, Queue emailQueue) {
        String routingKey = properties.getRabbitmq().getRoutingKey().getEmail();
        log.info("Binding email queue to exchange with routing key: {}", routingKey);
        return BindingBuilder.bind(emailQueue)
                .to(notificationExchange)
                .with(routingKey);
    }

    /**
     * Bind SMS queue to exchange with routing key.
     */
    @Bean
    public Binding smsBinding(TopicExchange notificationExchange, Queue smsQueue) {
        String routingKey = properties.getRabbitmq().getRoutingKey().getSms();
        log.info("Binding SMS queue to exchange with routing key: {}", routingKey);
        return BindingBuilder.bind(smsQueue)
                .to(notificationExchange)
                .with(routingKey);
    }

    /**
     * Bind WhatsApp queue to exchange with routing key.
     */
    @Bean
    public Binding whatsappBinding(TopicExchange notificationExchange, Queue whatsappQueue) {
        String routingKey = properties.getRabbitmq().getRoutingKey().getWhatsapp();
        log.info("Binding WhatsApp queue to exchange with routing key: {}", routingKey);
        return BindingBuilder.bind(whatsappQueue)
                .to(notificationExchange)
                .with(routingKey);
    }
}
