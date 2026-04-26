package ru.dobrovichek.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.dobrovichek.identity.entity.UserCredentialEntity;

import java.util.Optional;
import java.util.UUID;

public interface IdentityUserCredentialRepository extends JpaRepository<UserCredentialEntity, UUID> {

    Optional<UserCredentialEntity> findByPhoneNormalized(String phoneNormalized);

    boolean existsByPhoneNormalized(String phoneNormalized);
}
