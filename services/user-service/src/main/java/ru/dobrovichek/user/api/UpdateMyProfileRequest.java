package ru.dobrovichek.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
        @NotBlank @Size(min = 1, max = 60) @Pattern(regexp = "^[\\p{L}]([\\p{L}\\-']){0,59}$") String firstName,
        @NotBlank @Size(min = 1, max = 60) @Pattern(regexp = "^[\\p{L}]([\\p{L}\\-']){0,59}$") String lastName,
        @Size(max = 60) @Pattern(regexp = "^$|^[\\p{L}]([\\p{L}\\-']){0,59}$") String patronymic,
        @NotBlank @Pattern(regexp = "^[0-9+() -]{7,20}$") String phone,
        @Size(max = 1000) String bio,
        @Size(max = 120) String city
) {
}
