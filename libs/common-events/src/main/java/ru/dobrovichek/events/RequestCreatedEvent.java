package ru.dobrovichek.events;

import ru.dobrovichek.contracts.GeoPoint;

import java.time.Instant;
import java.util.UUID;

public record RequestCreatedEvent(
        UUID requestId,
        UUID wardId,
        GeoPoint location,
        Instant createdAt
) {
}
