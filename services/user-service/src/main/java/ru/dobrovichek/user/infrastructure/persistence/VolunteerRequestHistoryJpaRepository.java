package ru.dobrovichek.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.dobrovichek.contracts.RequestStatus;
import ru.dobrovichek.user.domain.VolunteerRequestHistory;

import java.util.List;
import java.util.UUID;

public interface VolunteerRequestHistoryJpaRepository extends JpaRepository<VolunteerRequestHistory, UUID> {

    List<VolunteerRequestHistory> findByVolunteerIdAndStatusOrderByCompletedAtDescUpdatedAtDesc(
            UUID volunteerId,
            RequestStatus status
    );
}
