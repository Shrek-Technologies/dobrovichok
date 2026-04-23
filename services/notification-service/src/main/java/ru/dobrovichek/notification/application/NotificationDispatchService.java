package ru.dobrovichek.notification.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.dobrovichek.contracts.RequestStatus;
import ru.dobrovichek.events.RequestCreatedEvent;
import ru.dobrovichek.events.RequestStatusChangedEvent;

@Service
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);

    public void handle(RequestCreatedEvent event) {
        log.info(
                "Dispatching volunteer notification for new request {} from ward {} at lat={}, lon={}",
                event.requestId(),
                event.wardId(),
                event.location().latitude(),
                event.location().longitude()
        );
    }

    public void handle(RequestStatusChangedEvent event) {
        if (event.status() == RequestStatus.ACCEPTED) {
            log.info(
                    "Dispatching ward notification: request {} accepted by volunteer {}",
                    event.requestId(),
                    event.volunteerId()
            );
            return;
        }

        if (event.status() == RequestStatus.CANCELLED) {
            if (event.volunteerId() != null) {
                log.info(
                        "Dispatching volunteer notification: request {} was cancelled by ward {}",
                        event.requestId(),
                        event.wardId()
                );
            }
            return;
        }

        if (event.status() == RequestStatus.COMPLETED) {
            log.info(
                    "Dispatching ward notification: request {} was completed by volunteer {}",
                    event.requestId(),
                    event.volunteerId()
            );
        }
    }
}
