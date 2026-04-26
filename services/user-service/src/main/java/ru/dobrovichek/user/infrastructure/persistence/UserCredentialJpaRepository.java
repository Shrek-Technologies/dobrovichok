package ru.dobrovichek.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.dobrovichek.user.domain.UserCredential;

import java.util.UUID;

public interface UserCredentialJpaRepository extends JpaRepository<UserCredential, UUID> {
}
