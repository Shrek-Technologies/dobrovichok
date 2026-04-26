package ru.dobrovichek.request.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import ru.dobrovichek.contracts.RequestStatus;
import ru.dobrovichek.request.application.port.out.HelpRequestRepository;
import ru.dobrovichek.request.domain.HelpRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class HelpRequestRepositoryAdapter implements HelpRequestRepository {

    private final HelpRequestJpaRepository repository;

    public HelpRequestRepositoryAdapter(HelpRequestJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<HelpRequest> findById(UUID requestId) {
        return repository.findById(requestId);
    }

    @Override
    public Optional<HelpRequest> findActiveForWard(UUID wardId) {
        return repository.findFirstByWardIdAndStatusInOrderByCreatedAtDesc(
                wardId,
                List.of(RequestStatus.CREATED, RequestStatus.ACCEPTED)
        );
    }

    @Override
    public Optional<HelpRequest> findActiveAcceptedForVolunteer(UUID volunteerId) {
        return repository.findFirstByVolunteerIdAndStatusOrderByCreatedAtDesc(
                volunteerId,
                RequestStatus.ACCEPTED
        );
    }

    @Override
    public HelpRequest save(HelpRequest request) {
        return repository.save(request);
    }

    @Override
    public List<HelpRequest> findNearby(
            double minLatitude,
            double maxLatitude,
            double minLongitude,
            double maxLongitude,
            Set<RequestStatus> statuses
    ) {
        return repository.findByStatusInAndLatitudeBetweenAndLongitudeBetween(
                statuses,
                minLatitude,
                maxLatitude,
                minLongitude,
                maxLongitude
        );
    }
}
