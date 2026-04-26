package ru.dobrovichek.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateVolunteerRatingRequest(
        @NotNull UUID requestId,
        @Min(1) @Max(5) int score
) {
}
