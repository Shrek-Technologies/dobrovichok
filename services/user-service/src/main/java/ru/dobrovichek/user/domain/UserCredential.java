package ru.dobrovichek.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_credentials")
public class UserCredential {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Column(name = "phone_normalized", nullable = false, length = 32)
    private String phoneNormalized;

    protected UserCredential() {
    }

    public static UserCredential create(UUID userId, String passwordHash, String phoneNormalized) {
        UserCredential c = new UserCredential();
        c.userId = Objects.requireNonNull(userId);
        c.passwordHash = Objects.requireNonNull(passwordHash);
        c.phoneNormalized = Objects.requireNonNull(phoneNormalized);
        return c;
    }

    public void updatePhoneNormalized(String phoneNormalized) {
        this.phoneNormalized = Objects.requireNonNull(phoneNormalized);
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPhoneNormalized() {
        return phoneNormalized;
    }
}
