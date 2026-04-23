package ru.dobrovichek.user.api;

import ru.dobrovichek.contracts.UserRole;

import java.util.UUID;

public record CurrentUser(
        UUID userId,
        UserRole role
) {
}
