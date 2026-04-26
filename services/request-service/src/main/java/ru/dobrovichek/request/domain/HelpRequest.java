package ru.dobrovichek.request.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.dobrovichek.contracts.GeoPoint;
import ru.dobrovichek.contracts.RequestStatus;
import ru.dobrovichek.request.application.ConflictException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "help_requests")
public class HelpRequest {

    @Id
    private UUID id;

    @Column(name = "ward_id", nullable = false, updatable = false)
    private UUID wardId;

    @Column(name = "volunteer_id")
    private UUID volunteerId;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "contact_phone", nullable = false, length = 32)
    private String contactPhone;

    @Column(name = "ward_first_name", nullable = false, length = 60)
    private String wardFirstName;

    @Column(name = "ward_last_name", nullable = false, length = 60)
    private String wardLastName;

    @Column(name = "ward_patronymic", length = 60)
    private String wardPatronymic;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private RequestStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    protected HelpRequest() {
    }

    public static HelpRequest create(
            UUID wardId,
            String description,
            String contactPhone,
            String wardFirstName,
            String wardLastName,
            String wardPatronymic,
            GeoPoint location,
            Instant now
    ) {
        HelpRequest request = new HelpRequest();
        request.id = UUID.randomUUID();
        request.wardId = Objects.requireNonNull(wardId);
        request.description = Objects.requireNonNull(description).trim();
        request.contactPhone = Objects.requireNonNull(contactPhone).trim();
        request.wardFirstName = Objects.requireNonNull(wardFirstName).trim();
        request.wardLastName = Objects.requireNonNull(wardLastName).trim();
        request.wardPatronymic = normalizeOptional(wardPatronymic);
        request.latitude = location.latitude();
        request.longitude = location.longitude();
        request.status = RequestStatus.CREATED;
        request.createdAt = now;
        request.updatedAt = now;
        return request;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    public void accept(UUID volunteerId, Instant now) {
        if (status != RequestStatus.CREATED) {
            throw new ConflictException("Only created requests can be accepted");
        }
        this.volunteerId = Objects.requireNonNull(volunteerId);
        this.status = RequestStatus.ACCEPTED;
        this.acceptedAt = now;
        this.updatedAt = now;
    }

    public void cancel(Instant now) {
        if (status == RequestStatus.CANCELLED) {
            throw new ConflictException("Request is already cancelled");
        }
        if (status == RequestStatus.COMPLETED) {
            throw new ConflictException("Completed request cannot be cancelled");
        }
        this.status = RequestStatus.CANCELLED;
        this.cancelledAt = now;
        this.updatedAt = now;
    }

    public void complete(Instant now) {
        if (status != RequestStatus.ACCEPTED) {
            throw new ConflictException("Only accepted requests can be completed");
        }
        this.status = RequestStatus.COMPLETED;
        this.completedAt = now;
        this.updatedAt = now;
    }

    public void abandonByVolunteer(Instant now) {
        if (status != RequestStatus.ACCEPTED) {
            throw new ConflictException("Only accepted requests can be released");
        }
        if (volunteerId == null) {
            throw new ConflictException("Request has no assigned volunteer");
        }
        this.volunteerId = null;
        this.status = RequestStatus.CREATED;
        this.acceptedAt = null;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWardId() {
        return wardId;
    }

    public UUID getVolunteerId() {
        return volunteerId;
    }

    public String getDescription() {
        return description;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getWardFirstName() {
        return wardFirstName;
    }

    public String getWardLastName() {
        return wardLastName;
    }

    public String getWardPatronymic() {
        return wardPatronymic;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public GeoPoint getLocation() {
        return new GeoPoint(latitude, longitude);
    }

    public RequestStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
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
}
