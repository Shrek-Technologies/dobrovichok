package ru.dobrovichek.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.dobrovichek.contracts.PersonNameFormat;
import ru.dobrovichek.contracts.UserRole;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
public class UserProfileEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserRole role;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 60)
    private String lastName;

    @Column(name = "patronymic", length = 60)
    private String patronymic;

    @Column(nullable = false, length = 32)
    private String phone;

    @Column(length = 1000)
    private String bio;

    @Column(length = 120)
    private String city;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount;

    @Column(name = "completed_requests_count", nullable = false)
    private int completedRequestsCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "fcm_token", length = 512)
    private String fcmToken;

    protected UserProfileEntity() {
    }

    public static UserProfileEntity forNewRegistration(
            UUID id,
            UserRole role,
            String firstName,
            String lastName,
            String patronymic,
            String phoneNormalized,
            Instant now
    ) {
        UserProfileEntity e = new UserProfileEntity();
        e.id = id;
        e.role = role;
        e.firstName = firstName;
        e.lastName = lastName;
        e.patronymic = patronymic;
        e.fullName = PersonNameFormat.fullFormal(firstName, patronymic, lastName);
        e.phone = phoneNormalized;
        e.bio = null;
        e.city = null;
        e.rating = BigDecimal.ZERO.setScale(2);
        e.ratingCount = 0;
        e.completedRequestsCount = 0;
        e.createdAt = now;
        e.updatedAt = now;
        e.fcmToken = null;
        return e;
    }

    public UUID getId() {
        return id;
    }

    public UserRole getRole() {
        return role;
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
}
