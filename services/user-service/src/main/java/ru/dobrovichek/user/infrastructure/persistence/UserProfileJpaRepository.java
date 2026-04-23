package ru.dobrovichek.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.dobrovichek.user.domain.UserProfile;

import java.util.UUID;

public interface UserProfileJpaRepository extends JpaRepository<UserProfile, UUID> {
}
