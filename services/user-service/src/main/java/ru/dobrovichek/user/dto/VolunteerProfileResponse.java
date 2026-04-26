package ru.dobrovichek.user.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VolunteerProfileResponse(
        UUID id,
        String firstName,
        String lastName,
        String patronymic,
        String fullName,
        String phone,
        String bio,
        String city,
        BigDecimal rating,
        int ratingCount,
        int completedRequestsCount,
        Instant updatedAt
) {
}
