package ru.dobrovichek.user.dto;

public record VolunteerRatingSnapshot(
        long ratingCount,
        Double averageScore
) {
}
