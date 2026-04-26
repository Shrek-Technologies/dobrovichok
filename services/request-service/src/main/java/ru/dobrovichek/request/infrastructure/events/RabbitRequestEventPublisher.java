package ru.dobrovichek.request.infrastructure.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import ru.dobrovichek.events.RequestCreatedEvent;
import ru.dobrovichek.events.RequestEventTopology;
import ru.dobrovichek.events.RequestStatusChangedEvent;
import ru.dobrovichek.events.VolunteerAbandonedRequestEvent;
import ru.dobrovichek.request.application.port.out.RequestEventPublisher;
import ru.dobrovichek.request.domain.HelpRequest;

import java.time.Instant;
import java.util.UUID;

public class RabbitRequestEventPublisher implements RequestEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitRequestEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitRequestEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishCreated(HelpRequest request) {
        publish(
                RequestEventTopology.REQUEST_CREATED_ROUTING_KEY,
                new RequestCreatedEvent(
                        request.getId(),
                        request.getWardId(),
                        request.getLocation(),
                        request.getCreatedAt()
                ),
                request.getId()
        );
    }

    @Override
    public void publishStatusChanged(HelpRequest request) {
        publish(
                RequestEventTopology.REQUEST_STATUS_CHANGED_ROUTING_KEY,
                new RequestStatusChangedEvent(
                        request.getId(),
                        request.getWardId(),
                        request.getVolunteerId(),
                        request.getStatus(),
                        request.getUpdatedAt()
                ),
                request.getId()
        );
    }

    @Override
    public void publishVolunteerAbandoned(UUID requestId, UUID wardId, UUID volunteerId, Instant abandonedAt) {
        publish(
                RequestEventTopology.REQUEST_VOLUNTEER_ABANDONED_ROUTING_KEY,
                new VolunteerAbandonedRequestEvent(requestId, wardId, volunteerId, abandonedAt),
                requestId
        );
    }

    private void publish(String routingKey, Object payload, Object requestId) {
        try {
            rabbitTemplate.convertAndSend(RequestEventTopology.REQUEST_EVENTS_EXCHANGE, routingKey, payload);
        } catch (AmqpException exception) {
            log.error("Failed to publish request event for request {}", requestId, exception);
        }
    }
}
