package ru.dobrovichek.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ru.dobrovichek.contracts.RequestStatus;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RequestSnapshot(
        UUID id,
        UUID wardId,
        UUID volunteerId,
        RequestStatus status,
        Instant acceptedAt,
        Instant completedAt
) {
}
