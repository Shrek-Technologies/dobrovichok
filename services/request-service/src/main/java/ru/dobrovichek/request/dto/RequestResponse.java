package ru.dobrovichek.request.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import ru.dobrovichek.contracts.GeoPoint;
import ru.dobrovichek.contracts.RequestStatus;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RequestResponse(
        UUID id,
        UUID wardId,
        UUID volunteerId,
        String description,
        String contactPhone,
        String wardFirstName,
        String wardLastName,
        String wardPatronymic,
        String wardFullName,
        GeoPoint location,
        RequestStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant acceptedAt,
        Instant completedAt,
        Instant cancelledAt
) {
}
