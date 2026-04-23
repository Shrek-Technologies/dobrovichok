package ru.dobrovichek.user.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.dobrovichek.events.RequestEventTopology;

@Configuration
@EnableRabbit
@ConditionalOnProperty(name = "dobrovichek.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitUserEventConfiguration {

    @Bean
    public TopicExchange requestEventsExchange() {
        return new TopicExchange(RequestEventTopology.REQUEST_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue userRequestStatusChangedQueue() {
        return QueueBuilder.durable(RequestEventTopology.USER_REQUEST_STATUS_CHANGED_QUEUE).build();
    }

    @Bean
    public Binding userRequestStatusChangedBinding(
            @Qualifier("userRequestStatusChangedQueue") Queue userRequestStatusChangedQueue,
            TopicExchange requestEventsExchange
    ) {
        return BindingBuilder.bind(userRequestStatusChangedQueue)
                .to(requestEventsExchange)
                .with(RequestEventTopology.REQUEST_STATUS_CHANGED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
