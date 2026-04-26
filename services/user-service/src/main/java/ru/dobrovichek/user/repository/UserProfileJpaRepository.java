package ru.dobrovichek.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.dobrovichek.user.entity.UserProfile;

import java.util.UUID;

public interface UserProfileJpaRepository extends JpaRepository<UserProfile, UUID> {
}
