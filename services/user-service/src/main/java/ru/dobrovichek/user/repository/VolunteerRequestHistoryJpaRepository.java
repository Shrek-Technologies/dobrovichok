package ru.dobrovichek.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.dobrovichek.contracts.RequestStatus;
import ru.dobrovichek.user.entity.VolunteerRequestHistory;

import java.util.List;
import java.util.UUID;

public interface VolunteerRequestHistoryJpaRepository extends JpaRepository<VolunteerRequestHistory, UUID> {

    List<VolunteerRequestHistory> findByVolunteerIdAndStatusOrderByCompletedAtDescUpdatedAtDesc(
            UUID volunteerId,
            RequestStatus status
    );
}
