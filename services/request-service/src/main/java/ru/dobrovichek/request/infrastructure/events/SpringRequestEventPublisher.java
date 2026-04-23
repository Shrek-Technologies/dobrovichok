package ru.dobrovichek.request.infrastructure.events;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import ru.dobrovichek.events.RequestCreatedEvent;
import ru.dobrovichek.events.RequestStatusChangedEvent;
import ru.dobrovichek.request.application.port.out.RequestEventPublisher;
import ru.dobrovichek.request.domain.HelpRequest;

@Component
public class SpringRequestEventPublisher implements RequestEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringRequestEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publishCreated(HelpRequest request) {
        applicationEventPublisher.publishEvent(new RequestCreatedEvent(
                request.getId(),
                request.getWardId(),
                request.getLocation(),
                request.getCreatedAt()
        ));
    }

    @Override
    public void publishStatusChanged(HelpRequest request) {
        applicationEventPublisher.publishEvent(new RequestStatusChangedEvent(
                request.getId(),
                request.getStatus(),
                request.getUpdatedAt()
        ));
    }
}
