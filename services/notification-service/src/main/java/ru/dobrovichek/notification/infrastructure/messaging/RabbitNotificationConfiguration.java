package ru.dobrovichek.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import ru.dobrovichek.events.RequestEventTopology;

@Configuration
@EnableRabbit
@ConditionalOnProperty(name = "dobrovichek.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitNotificationConfiguration {

    @Bean
    public TopicExchange requestEventsExchange() {
        return new TopicExchange(RequestEventTopology.REQUEST_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue requestCreatedQueue() {
        return QueueBuilder.durable(RequestEventTopology.REQUEST_CREATED_QUEUE).build();
    }

    @Bean
    public Queue requestStatusChangedQueue() {
        return QueueBuilder.durable(RequestEventTopology.REQUEST_STATUS_CHANGED_QUEUE).build();
    }

    @Bean
    public Queue notificationVolunteerAbandonedQueue() {
        return QueueBuilder.durable(RequestEventTopology.NOTIFICATION_REQUEST_VOLUNTEER_ABANDONED_QUEUE).build();
    }

    @Bean
    public Binding requestCreatedBinding(
            @Qualifier("requestCreatedQueue") Queue requestCreatedQueue,
            TopicExchange requestEventsExchange
    ) {
        return BindingBuilder.bind(requestCreatedQueue)
                .to(requestEventsExchange)
                .with(RequestEventTopology.REQUEST_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding requestStatusChangedBinding(
            @Qualifier("requestStatusChangedQueue") Queue requestStatusChangedQueue,
            TopicExchange requestEventsExchange
    ) {
        return BindingBuilder.bind(requestStatusChangedQueue)
                .to(requestEventsExchange)
                .with(RequestEventTopology.REQUEST_STATUS_CHANGED_ROUTING_KEY);
    }

    @Bean
    public Binding notificationVolunteerAbandonedBinding(
            @Qualifier("notificationVolunteerAbandonedQueue") Queue notificationVolunteerAbandonedQueue,
            TopicExchange requestEventsExchange
    ) {
        return BindingBuilder.bind(notificationVolunteerAbandonedQueue)
                .to(requestEventsExchange)
                .with(RequestEventTopology.REQUEST_VOLUNTEER_ABANDONED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
