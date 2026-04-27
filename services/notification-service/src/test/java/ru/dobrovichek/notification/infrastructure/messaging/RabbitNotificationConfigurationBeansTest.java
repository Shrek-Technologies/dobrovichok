package ru.dobrovichek.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import ru.dobrovichek.events.RequestEventTopology;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitNotificationConfigurationBeansTest {

    private final RabbitNotificationConfiguration config = new RabbitNotificationConfiguration();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void exchangeAndQueuesUseTopologyNames() {
        TopicExchange exchange = config.requestEventsExchange();
        assertThat(exchange.getName()).isEqualTo(RequestEventTopology.REQUEST_EVENTS_EXCHANGE);

        Queue created = config.requestCreatedQueue();
        assertThat(created.getName()).isEqualTo(RequestEventTopology.REQUEST_CREATED_QUEUE);

        Queue status = config.requestStatusChangedQueue();
        assertThat(status.getName()).isEqualTo(RequestEventTopology.REQUEST_STATUS_CHANGED_QUEUE);

        Queue abandoned = config.notificationVolunteerAbandonedQueue();
        assertThat(abandoned.getName()).isEqualTo(RequestEventTopology.NOTIFICATION_REQUEST_VOLUNTEER_ABANDONED_QUEUE);
    }

    @Test
    void bindingsUseExpectedRoutingKeys() {
        TopicExchange exchange = config.requestEventsExchange();
        Queue created = config.requestCreatedQueue();
        Queue status = config.requestStatusChangedQueue();
        Queue abandoned = config.notificationVolunteerAbandonedQueue();

        Binding b1 = config.requestCreatedBinding(created, exchange);
        assertThat(b1.getRoutingKey()).isEqualTo(RequestEventTopology.REQUEST_CREATED_ROUTING_KEY);

        Binding b2 = config.requestStatusChangedBinding(status, exchange);
        assertThat(b2.getRoutingKey()).isEqualTo(RequestEventTopology.REQUEST_STATUS_CHANGED_ROUTING_KEY);

        Binding b3 = config.notificationVolunteerAbandonedBinding(abandoned, exchange);
        assertThat(b3.getRoutingKey()).isEqualTo(RequestEventTopology.REQUEST_VOLUNTEER_ABANDONED_ROUTING_KEY);
    }

    @Test
    void rabbitMessageConverterIsJackson() {
        assertThat(config.rabbitMessageConverter(objectMapper))
                .isInstanceOf(Jackson2JsonMessageConverter.class);
    }
}
