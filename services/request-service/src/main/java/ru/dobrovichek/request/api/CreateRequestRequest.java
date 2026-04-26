package ru.dobrovichek.request.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.dobrovichek.contracts.GeoPoint;

public record CreateRequestRequest(
        @NotBlank @Size(max = 1000) String description,
        @NotBlank @Pattern(regexp = "^[0-9+() -]{7,20}$") String contactPhone,
        @NotBlank @Size(min = 1, max = 60) @Pattern(regexp = "^[\\p{L}]([\\p{L}\\-']){0,59}$") String wardFirstName,
        @NotBlank @Size(min = 1, max = 60) @Pattern(regexp = "^[\\p{L}]([\\p{L}\\-']){0,59}$") String wardLastName,
        @Size(max = 60) @Pattern(regexp = "^$|^[\\p{L}]([\\p{L}\\-']){0,59}$") String wardPatronymic,
        @NotNull @Valid GeoPoint location
) {
}
