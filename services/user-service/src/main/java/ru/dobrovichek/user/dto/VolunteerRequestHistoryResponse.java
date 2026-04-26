package ru.dobrovichek.user.dto;

import ru.dobrovichek.contracts.RequestStatus;

import java.time.Instant;
import java.util.UUID;

public record VolunteerRequestHistoryResponse(
        UUID requestId,
        UUID wardId,
        RequestStatus status,
        Instant acceptedAt,
        Instant completedAt,
        Instant cancelledAt,
        Instant updatedAt,
        String category,
        String address,
        Integer wardRating
) {
}
