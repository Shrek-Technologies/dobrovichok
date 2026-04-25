package ru.dobrovichek.identity.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import ru.dobrovichek.contracts.UserRole;

public record RegisterRequest(
        @NotBlank String fullName,
        @NotBlank @Pattern(regexp = "^[0-9+() -]{7,20}$") String phone,
        @NotBlank String password,
        UserRole role
) {
}
