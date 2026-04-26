package ru.dobrovichek.identity.api;

import ru.dobrovichek.contracts.UserRole;

import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String firstName,
        String lastName,
        String patronymic,
        String fullName,
        String phone,
        UserRole role
) {
}
