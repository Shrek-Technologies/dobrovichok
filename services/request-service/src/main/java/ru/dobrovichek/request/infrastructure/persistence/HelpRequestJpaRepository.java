package ru.dobrovichek.request.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.dobrovichek.contracts.RequestStatus;
import ru.dobrovichek.request.domain.HelpRequest;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HelpRequestJpaRepository extends JpaRepository<HelpRequest, UUID> {

    List<HelpRequest> findByStatusInAndLatitudeBetweenAndLongitudeBetween(
            Collection<RequestStatus> statuses,
            double minLatitude,
            double maxLatitude,
            double minLongitude,
            double maxLongitude
    );

    Optional<HelpRequest> findFirstByWardIdAndStatusInOrderByCreatedAtDesc(
            UUID wardId,
            Collection<RequestStatus> statuses
    );

    Optional<HelpRequest> findFirstByVolunteerIdAndStatusOrderByCreatedAtDesc(
            UUID volunteerId,
            RequestStatus status
    );
}
