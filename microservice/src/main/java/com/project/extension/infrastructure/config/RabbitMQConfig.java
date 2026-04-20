package com.project.extension.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NAME = "fila.orcamento.pdf";
    public static final String RESPONSE_QUEUE_NAME = "fila.orcamento.pdf.resposta";
    public static final String DLQ_NAME = "fila.orcamento.pdf.falha";
    public static final String EXCHANGE_NAME = "exchange.leovidros.direct";
    public static final String DLX_NAME = "exchange.leovidros.dlx";
    public static final String ROUTING_KEY = "orcamento.gerar";
    public static final String RESPONSE_ROUTING_KEY = "orcamento.resposta";
    public static final String DEAD_LETTER_ROUTING_KEY = "orcamento.falha";

    @Bean
    public Queue orcamentoQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .deadLetterExchange(DLX_NAME)
                .deadLetterRoutingKey(DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue orcamentoResponseQueue() {
        return new Queue(RESPONSE_QUEUE_NAME, true);
    }

    @Bean
    public Queue orcamentoDeadLetterQueue() {
        return QueueBuilder.durable(DLQ_NAME).build();
    }

    @Bean
    public DirectExchange leoVidrosExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public DirectExchange leoVidrosDeadLetterExchange() {
        return new DirectExchange(DLX_NAME);
    }

    @Bean
    public Binding orcamentoBinding(Queue orcamentoQueue, DirectExchange leoVidrosExchange) {
        return BindingBuilder.bind(orcamentoQueue).to(leoVidrosExchange).with(ROUTING_KEY);
    }

    @Bean
    public Binding orcamentoResponseBinding(Queue orcamentoResponseQueue, DirectExchange leoVidrosExchange) {
        return BindingBuilder.bind(orcamentoResponseQueue).to(leoVidrosExchange).with(RESPONSE_ROUTING_KEY);
    }

    @Bean
    public Binding orcamentoDeadLetterBinding(Queue orcamentoDeadLetterQueue,
                                              DirectExchange leoVidrosDeadLetterExchange) {
        return BindingBuilder.bind(orcamentoDeadLetterQueue)
                .to(leoVidrosDeadLetterExchange)
                .with(DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}