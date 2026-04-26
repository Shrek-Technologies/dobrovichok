package ru.dobrovichek.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.dobrovichek.identity.entity.UserProfileEntity;

import java.util.UUID;

public interface IdentityUserProfileRepository extends JpaRepository<UserProfileEntity, UUID> {

    boolean existsByPhone(String phone);
}
