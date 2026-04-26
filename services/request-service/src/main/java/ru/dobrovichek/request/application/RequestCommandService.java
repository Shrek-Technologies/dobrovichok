package ru.dobrovichek.request.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.dobrovichek.contracts.UserRole;
import ru.dobrovichek.request.api.CreateRequestRequest;
import ru.dobrovichek.request.api.CurrentUser;
import ru.dobrovichek.request.application.port.out.HelpRequestRepository;
import ru.dobrovichek.request.application.port.out.RequestEventPublisher;
import ru.dobrovichek.request.domain.HelpRequest;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class RequestCommandService {

    private final HelpRequestRepository requestRepository;
    private final RequestEventPublisher eventPublisher;
    private final Clock clock;

    public RequestCommandService(
            HelpRequestRepository requestRepository,
            RequestEventPublisher eventPublisher,
            Clock clock
    ) {
        this.requestRepository = requestRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public HelpRequest create(CurrentUser currentUser, CreateRequestRequest command) {
        requireRole(currentUser, UserRole.WARD, "Only wards can create requests");

        HelpRequest request = HelpRequest.create(
                currentUser.userId(),
                command.description(),
                command.contactPhone(),
                command.wardFirstName(),
                command.wardLastName(),
                command.wardPatronymic(),
                command.location(),
                Instant.now(clock)
        );

        HelpRequest saved = requestRepository.save(request);
        eventPublisher.publishCreated(saved);
        return saved;
    }

    @Transactional
    public HelpRequest accept(UUID requestId, CurrentUser currentUser) {
        requireRole(currentUser, UserRole.VOLUNTEER, "Only volunteers can accept requests");

        HelpRequest request = findExisting(requestId);
        request.accept(currentUser.userId(), Instant.now(clock));

        HelpRequest saved = requestRepository.save(request);
        eventPublisher.publishStatusChanged(saved);
        return saved;
    }

    @Transactional
    public HelpRequest cancel(UUID requestId, CurrentUser currentUser) {
        requireRole(currentUser, UserRole.WARD, "Only wards can cancel requests");

        HelpRequest request = findExisting(requestId);
        if (!request.getWardId().equals(currentUser.userId())) {
            throw new ForbiddenException("You can cancel only your own requests");
        }

        request.cancel(Instant.now(clock));

        HelpRequest saved = requestRepository.save(request);
        eventPublisher.publishStatusChanged(saved);
        return saved;
    }

    @Transactional
    public HelpRequest complete(UUID requestId, CurrentUser currentUser) {
        requireRole(currentUser, UserRole.VOLUNTEER, "Only volunteers can complete requests");

        HelpRequest request = findExisting(requestId);
        if (!currentUser.userId().equals(request.getVolunteerId())) {
            throw new ForbiddenException("Only the assigned volunteer can complete the request");
        }

        request.complete(Instant.now(clock));

        HelpRequest saved = requestRepository.save(request);
        eventPublisher.publishStatusChanged(saved);
        return saved;
    }

    private HelpRequest findExisting(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));
    }

    private void requireRole(CurrentUser currentUser, UserRole expectedRole, String message) {
        if (currentUser.role() != expectedRole) {
            throw new ForbiddenException(message);
        }
    }
}
