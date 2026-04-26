package ru.dobrovichek.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.dobrovichek.contracts.RequestStatus;
import ru.dobrovichek.request.entity.HelpRequest;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface HelpRequestRepository extends JpaRepository<HelpRequest, UUID> {

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

    default Optional<HelpRequest> findActiveForWard(UUID wardId) {
        return findFirstByWardIdAndStatusInOrderByCreatedAtDesc(
                wardId,
                List.of(RequestStatus.CREATED, RequestStatus.ACCEPTED)
        );
    }

    default Optional<HelpRequest> findActiveAcceptedForVolunteer(UUID volunteerId) {
        return findFirstByVolunteerIdAndStatusOrderByCreatedAtDesc(volunteerId, RequestStatus.ACCEPTED);
    }

    default List<HelpRequest> findNearby(
            double minLatitude,
            double maxLatitude,
            double minLongitude,
            double maxLongitude,
            Set<RequestStatus> statuses
    ) {
        return findByStatusInAndLatitudeBetweenAndLongitudeBetween(
                statuses,
                minLatitude,
                maxLatitude,
                minLongitude,
                maxLongitude
        );
    }
}
