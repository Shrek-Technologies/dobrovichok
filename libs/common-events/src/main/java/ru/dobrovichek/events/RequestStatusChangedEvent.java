package ru.dobrovichek.events;

import ru.dobrovichek.contracts.RequestStatus;

import java.time.Instant;
import java.util.UUID;

public record RequestStatusChangedEvent(
        UUID requestId,
        RequestStatus status,
        Instant changedAt
) {
}
