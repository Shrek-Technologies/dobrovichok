package ru.dobrovichek.request.dto;

import ru.dobrovichek.contracts.UserRole;

import java.util.UUID;

public record CurrentUser(
        UUID userId,
        UserRole role
) {
}
