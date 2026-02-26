package com.project.extension.infrastructure.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String FILA_ORCAMENTO_PDF = "fila.orcamento.pdf";

    @Bean
    public Queue orcamentoQueue() {
        return new Queue(FILA_ORCAMENTO_PDF, true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        // O Spring Boot vai injetar o ObjectMapper aqui automaticamente
        // assim que a dependência do Jackson estiver no pom.xml
        return new Jackson2JsonMessageConverter();
    }
}