package ru.dobrovichek.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.dobrovichek.contracts.PersonNameFormat;
import ru.dobrovichek.contracts.UserRole;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private UserRole role;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 60)
    private String lastName;

    @Column(name = "patronymic", length = 60)
    private String patronymic;

    @Column(name = "phone", nullable = false, length = 32)
    private String phone;

    @Column(name = "bio", length = 1000)
    private String bio;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount;

    @Column(name = "completed_requests_count", nullable = false)
    private int completedRequestsCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserProfile() {
    }

    public static UserProfile create(UUID id, UserRole role, Instant now) {
        UserProfile profile = new UserProfile();
        profile.id = Objects.requireNonNull(id);
        profile.role = Objects.requireNonNull(role);
        profile.fullName = "";
        profile.firstName = "";
        profile.lastName = "";
        profile.patronymic = null;
        profile.phone = "";
        profile.rating = BigDecimal.ZERO.setScale(2);
        profile.ratingCount = 0;
        profile.completedRequestsCount = 0;
        profile.createdAt = now;
        profile.updatedAt = now;
        return profile;
    }

    public void updateProfile(
            String firstName,
            String lastName,
            String patronymic,
            String phone,
            String bio,
            String city,
            Instant now
    ) {
        this.firstName = Objects.requireNonNull(firstName).trim();
        this.lastName = Objects.requireNonNull(lastName).trim();
        this.patronymic = normalize(patronymic);
        this.fullName = PersonNameFormat.fullFormal(this.firstName, this.patronymic, this.lastName);
        this.phone = Objects.requireNonNull(phone).trim();
        this.bio = normalize(bio);
        this.city = normalize(city);
        this.updatedAt = now;
    }

    public void registerCompletedRequest(Instant now) {
        if (role != UserRole.VOLUNTEER) {
            throw new IllegalStateException("Only volunteer profile can register completed requests");
        }
        completedRequestsCount += 1;
        updatedAt = now;
    }

    public void updateRating(BigDecimal rating, int ratingCount, Instant now) {
        if (role != UserRole.VOLUNTEER) {
            throw new IllegalStateException("Only volunteer profile can have rating");
        }
        this.rating = Objects.requireNonNull(rating).setScale(2, RoundingMode.HALF_UP);
        this.ratingCount = ratingCount;
        this.updatedAt = now;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public UUID getId() {
        return id;
    }

    public UserRole getRole() {
        return role;
    }

    public String getFullName() {
        return fullName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPatronymic() {
        return patronymic;
    }

    public String getPhone() {
        return phone;
    }

    public String getBio() {
        return bio;
    }

    public String getCity() {
        return city;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public int getCompletedRequestsCount() {
        return completedRequestsCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
