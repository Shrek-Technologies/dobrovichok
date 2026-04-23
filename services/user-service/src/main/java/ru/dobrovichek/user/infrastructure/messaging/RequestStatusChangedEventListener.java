package ru.dobrovichek.user.infrastructure.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.dobrovichek.events.RequestEventTopology;
import ru.dobrovichek.events.RequestStatusChangedEvent;
import ru.dobrovichek.user.application.VolunteerHistoryProjector;

@Component
@ConditionalOnProperty(name = "dobrovichek.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class RequestStatusChangedEventListener {

    private final VolunteerHistoryProjector volunteerHistoryProjector;

    public RequestStatusChangedEventListener(VolunteerHistoryProjector volunteerHistoryProjector) {
        this.volunteerHistoryProjector = volunteerHistoryProjector;
    }

    @RabbitListener(queues = RequestEventTopology.USER_REQUEST_STATUS_CHANGED_QUEUE)
    public void handle(RequestStatusChangedEvent event) {
        volunteerHistoryProjector.project(event);
    }
}
