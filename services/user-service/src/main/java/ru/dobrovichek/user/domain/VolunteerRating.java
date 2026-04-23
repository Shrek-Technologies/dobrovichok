package ru.dobrovichek.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "volunteer_ratings")
public class VolunteerRating {

    @Id
    private UUID id;

    @Column(name = "request_id", nullable = false, unique = true, updatable = false)
    private UUID requestId;

    @Column(name = "volunteer_id", nullable = false, updatable = false)
    private UUID volunteerId;

    @Column(name = "ward_id", nullable = false, updatable = false)
    private UUID wardId;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected VolunteerRating() {
    }

    public static VolunteerRating create(
            UUID requestId,
            UUID volunteerId,
            UUID wardId,
            int score,
            Instant createdAt
    ) {
        VolunteerRating rating = new VolunteerRating();
        rating.id = UUID.randomUUID();
        rating.requestId = Objects.requireNonNull(requestId);
        rating.volunteerId = Objects.requireNonNull(volunteerId);
        rating.wardId = Objects.requireNonNull(wardId);
        rating.score = score;
        rating.createdAt = Objects.requireNonNull(createdAt);
        return rating;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public UUID getVolunteerId() {
        return volunteerId;
    }

    public UUID getWardId() {
        return wardId;
    }

    public int getScore() {
        return score;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
