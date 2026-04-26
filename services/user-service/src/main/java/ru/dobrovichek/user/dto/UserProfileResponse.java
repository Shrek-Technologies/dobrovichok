package ru.dobrovichek.user.dto;

import ru.dobrovichek.contracts.UserRole;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        UserRole role,
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
        Instant createdAt,
        Instant updatedAt
) {
}
