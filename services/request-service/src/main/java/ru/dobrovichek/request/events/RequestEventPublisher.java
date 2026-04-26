package ru.dobrovichek.request.events;

import ru.dobrovichek.request.entity.HelpRequest;

import java.time.Instant;
import java.util.UUID;

public interface RequestEventPublisher {

    void publishCreated(HelpRequest request);

    void publishStatusChanged(HelpRequest request);

    void publishVolunteerAbandoned(UUID requestId, UUID wardId, UUID volunteerId, Instant abandonedAt);
}
