package ru.dobrovichek.user.api;

import java.time.Instant;
import java.util.UUID;

public record VolunteerRatingResponse(
        UUID id,
        UUID requestId,
        UUID volunteerId,
        UUID wardId,
        int score,
        Instant createdAt
) {
}
