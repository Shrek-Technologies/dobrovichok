package ru.dobrovichek.request.infrastructure.events;

import ru.dobrovichek.request.application.port.out.RequestEventPublisher;
import ru.dobrovichek.request.domain.HelpRequest;

public class NoOpRequestEventPublisher implements RequestEventPublisher {

    @Override
    public void publishCreated(HelpRequest request) {
    }

    @Override
    public void publishStatusChanged(HelpRequest request) {
    }
}
