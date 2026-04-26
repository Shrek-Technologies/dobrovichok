package ru.dobrovichek.user.dto;

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
