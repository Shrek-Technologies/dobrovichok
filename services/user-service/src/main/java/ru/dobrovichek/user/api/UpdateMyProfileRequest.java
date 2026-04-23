package ru.dobrovichek.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
        @NotBlank @Size(max = 200) String fullName,
        @NotBlank @Pattern(regexp = "^[0-9+() -]{7,20}$") String phone,
        @Size(max = 1000) String bio,
        @Size(max = 120) String city
) {
}
