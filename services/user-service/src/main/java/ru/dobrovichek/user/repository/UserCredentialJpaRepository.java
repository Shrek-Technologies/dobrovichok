package ru.dobrovichek.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.dobrovichek.user.entity.UserCredential;

import java.util.UUID;

public interface UserCredentialJpaRepository extends JpaRepository<UserCredential, UUID> {
}
