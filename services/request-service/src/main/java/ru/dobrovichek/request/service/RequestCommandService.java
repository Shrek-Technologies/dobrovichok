package ru.dobrovichek.request.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.dobrovichek.contracts.RequestStatus;
import ru.dobrovichek.contracts.UserRole;
import ru.dobrovichek.request.dto.CreateRequestRequest;
import ru.dobrovichek.request.dto.CurrentUser;
import ru.dobrovichek.request.repository.HelpRequestRepository;
import ru.dobrovichek.request.events.RequestEventPublisher;
import ru.dobrovichek.request.entity.HelpRequest;
import ru.dobrovichek.request.exception.ConflictException;
import ru.dobrovichek.request.exception.ForbiddenException;
import ru.dobrovichek.request.exception.RequestNotFoundException;

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
        HelpRequest request = findExisting(requestId);
        if (request.getStatus() != RequestStatus.ACCEPTED) {
            throw new ConflictException("Only accepted requests can be completed");
        }
        if (currentUser.role() == UserRole.VOLUNTEER) {
            if (!currentUser.userId().equals(request.getVolunteerId())) {
                throw new ForbiddenException("Only the assigned volunteer can complete the request");
            }
        } else if (currentUser.role() == UserRole.WARD) {
            if (!currentUser.userId().equals(request.getWardId())) {
                throw new ForbiddenException("Only the ward owner can complete this request");
            }
        } else {
            throw new ForbiddenException("Only ward or assigned volunteer can complete the request");
        }

        request.complete(Instant.now(clock));

        HelpRequest saved = requestRepository.save(request);
        eventPublisher.publishStatusChanged(saved);
        return saved;
    }

    @Transactional
    public HelpRequest abandonByVolunteer(UUID requestId, CurrentUser currentUser) {
        requireRole(currentUser, UserRole.VOLUNTEER, "Only volunteers can release an accepted request");

        HelpRequest request = findExisting(requestId);
        if (request.getStatus() != RequestStatus.ACCEPTED) {
            throw new ConflictException("Only accepted requests can be released");
        }
        if (!currentUser.userId().equals(request.getVolunteerId())) {
            throw new ForbiddenException("Only the assigned volunteer can release this request");
        }

        UUID wardId = request.getWardId();
        UUID volunteerId = request.getVolunteerId();
        Instant now = Instant.now(clock);
        request.abandonByVolunteer(now);

        HelpRequest saved = requestRepository.save(request);
        eventPublisher.publishVolunteerAbandoned(saved.getId(), wardId, volunteerId, now);
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
