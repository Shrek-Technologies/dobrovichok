package ru.dobrovichek.identity.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.dobrovichek.contracts.UserRole;

public record RegisterRequest(
        @NotBlank @Size(min = 1, max = 60) @Pattern(regexp = "^[\\p{L}]([\\p{L}\\-']){0,59}$") String firstName,
        @NotBlank @Size(min = 1, max = 60) @Pattern(regexp = "^[\\p{L}]([\\p{L}\\-']){0,59}$") String lastName,
        @Size(max = 60) @Pattern(regexp = "^$|^[\\p{L}]([\\p{L}\\-']){0,59}$") String patronymic,
        @NotBlank @Pattern(regexp = "^[0-9+() -]{7,20}$") String phone,
        @NotBlank @Size(min = 8, max = 128) @Pattern(regexp = "^(?=.*\\d)(?=.*\\p{Lu}).{8,128}$") String password,
        UserRole role
) {
}
