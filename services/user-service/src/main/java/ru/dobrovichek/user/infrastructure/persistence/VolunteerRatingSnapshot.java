package ru.dobrovichek.user.infrastructure.persistence;

public record VolunteerRatingSnapshot(
        long ratingCount,
        Double averageScore
) {
}
