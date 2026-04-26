package ru.dobrovichek.events;

import java.time.Instant;
import java.util.UUID;

public record VolunteerAbandonedRequestEvent(
        UUID requestId,
        UUID wardId,
        UUID volunteerId,
        Instant abandonedAt
) {
}
