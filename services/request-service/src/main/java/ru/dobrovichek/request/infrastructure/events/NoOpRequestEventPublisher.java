package ru.dobrovichek.request.infrastructure.events;

import ru.dobrovichek.request.application.port.out.RequestEventPublisher;
import ru.dobrovichek.request.domain.HelpRequest;

import java.time.Instant;
import java.util.UUID;

public class NoOpRequestEventPublisher implements RequestEventPublisher {

    @Override
    public void publishCreated(HelpRequest request) {
    }

    @Override
    public void publishStatusChanged(HelpRequest request) {
    }

    @Override
    public void publishVolunteerAbandoned(UUID requestId, UUID wardId, UUID volunteerId, Instant abandonedAt) {
    }
}
