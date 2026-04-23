package ru.dobrovichek.request.api;

import ru.dobrovichek.contracts.GeoPoint;
import ru.dobrovichek.contracts.RequestStatus;

import java.time.Instant;
import java.util.UUID;

public record RequestSummaryResponse(
        UUID id,
        UUID wardId,
        String description,
        GeoPoint location,
        RequestStatus status,
        Instant createdAt,
        double distanceKm
) {
}
