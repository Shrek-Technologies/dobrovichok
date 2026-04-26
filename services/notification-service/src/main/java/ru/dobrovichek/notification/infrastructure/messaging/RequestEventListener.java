package ru.dobrovichek.notification.infrastructure.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.dobrovichek.events.RequestCreatedEvent;
import ru.dobrovichek.events.RequestEventTopology;
import ru.dobrovichek.events.RequestStatusChangedEvent;
import ru.dobrovichek.events.VolunteerAbandonedRequestEvent;
import ru.dobrovichek.notification.application.NotificationDispatchService;

@Component
@ConditionalOnProperty(name = "dobrovichek.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class RequestEventListener {

    private final NotificationDispatchService notificationDispatchService;

    public RequestEventListener(NotificationDispatchService notificationDispatchService) {
        this.notificationDispatchService = notificationDispatchService;
    }

    @RabbitListener(queues = RequestEventTopology.REQUEST_CREATED_QUEUE)
    public void handleRequestCreated(RequestCreatedEvent event) {
        notificationDispatchService.handle(event);
    }

    @RabbitListener(queues = RequestEventTopology.REQUEST_STATUS_CHANGED_QUEUE)
    public void handleRequestStatusChanged(RequestStatusChangedEvent event) {
        notificationDispatchService.handle(event);
    }

    @RabbitListener(queues = RequestEventTopology.NOTIFICATION_REQUEST_VOLUNTEER_ABANDONED_QUEUE)
    public void handleVolunteerAbandoned(VolunteerAbandonedRequestEvent event) {
        notificationDispatchService.handle(event);
    }
}
