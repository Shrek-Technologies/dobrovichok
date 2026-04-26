package ru.dobrovichek.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.dobrovichek.contracts.RequestStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "volunteer_request_history")
public class VolunteerRequestHistory {

    @Id
    @Column(name = "request_id", nullable = false, updatable = false)
    private UUID requestId;

    @Column(name = "volunteer_id", nullable = false)
    private UUID volunteerId;

    @Column(name = "ward_id", nullable = false)
    private UUID wardId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private RequestStatus status;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VolunteerRequestHistory() {
    }

    public static VolunteerRequestHistory create(UUID requestId, UUID volunteerId, UUID wardId, Instant now) {
        VolunteerRequestHistory history = new VolunteerRequestHistory();
        history.requestId = Objects.requireNonNull(requestId);
        history.volunteerId = Objects.requireNonNull(volunteerId);
        history.wardId = Objects.requireNonNull(wardId);
        history.status = RequestStatus.ACCEPTED;
        history.acceptedAt = now;
        history.updatedAt = now;
        return history;
    }

    public void apply(RequestStatus status, Instant changedAt, UUID wardId, UUID volunteerId) {
        this.status = Objects.requireNonNull(status);
        this.updatedAt = Objects.requireNonNull(changedAt);
        this.wardId = Objects.requireNonNull(wardId);
        this.volunteerId = Objects.requireNonNull(volunteerId);

        if (status == RequestStatus.ACCEPTED && acceptedAt == null) {
            acceptedAt = changedAt;
        }
        if (status == RequestStatus.COMPLETED) {
            completedAt = changedAt;
        }
        if (status == RequestStatus.CANCELLED) {
            cancelledAt = changedAt;
        }
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

    public RequestStatus getStatus() {
        return status;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
