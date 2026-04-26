package ru.dobrovichek.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "user_credentials")
public class UserCredentialEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Column(name = "phone_normalized", nullable = false, length = 32, unique = true)
    private String phoneNormalized;

    protected UserCredentialEntity() {
    }

    public static UserCredentialEntity create(UUID userId, String passwordHash, String phoneNormalized) {
        UserCredentialEntity e = new UserCredentialEntity();
        e.userId = userId;
        e.passwordHash = passwordHash;
        e.phoneNormalized = phoneNormalized;
        return e;
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
