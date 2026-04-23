package ru.dobrovichek.user.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.dobrovichek.contracts.RequestStatus;
import ru.dobrovichek.events.RequestStatusChangedEvent;
import ru.dobrovichek.user.domain.UserProfile;
import ru.dobrovichek.user.domain.VolunteerRequestHistory;
import ru.dobrovichek.user.infrastructure.persistence.UserProfileJpaRepository;
import ru.dobrovichek.user.infrastructure.persistence.VolunteerRequestHistoryJpaRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class VolunteerHistoryProjector {

    private final VolunteerRequestHistoryJpaRepository volunteerRequestHistoryRepository;
    private final UserProfileJpaRepository userProfileRepository;
    private final UserProfileService userProfileService;
    private final Clock clock;

    public VolunteerHistoryProjector(
            VolunteerRequestHistoryJpaRepository volunteerRequestHistoryRepository,
            UserProfileJpaRepository userProfileRepository,
            UserProfileService userProfileService,
            Clock clock
    ) {
        this.volunteerRequestHistoryRepository = volunteerRequestHistoryRepository;
        this.userProfileRepository = userProfileRepository;
        this.userProfileService = userProfileService;
        this.clock = clock;
    }

    @Transactional
    public void project(RequestStatusChangedEvent event) {
        UUID volunteerId = event.volunteerId();
        if (volunteerId == null) {
            return;
        }

        VolunteerRequestHistory history = volunteerRequestHistoryRepository.findById(event.requestId())
                .orElseGet(() -> VolunteerRequestHistory.create(
                        event.requestId(),
                        volunteerId,
                        event.wardId(),
                        effectiveTime(event.changedAt())
                ));

        RequestStatus previousStatus = history.getStatus();
        history.apply(event.status(), effectiveTime(event.changedAt()), event.wardId(), volunteerId);
        volunteerRequestHistoryRepository.save(history);

        UserProfile volunteer = userProfileService.getOrCreateVolunteerShell(volunteerId, effectiveTime(event.changedAt()));
        if (event.status() == RequestStatus.COMPLETED && previousStatus != RequestStatus.COMPLETED) {
            volunteer.registerCompletedRequest(effectiveTime(event.changedAt()));
            userProfileRepository.save(volunteer);
        }
    }

    @Transactional(readOnly = true)
    public List<VolunteerRequestHistory> findCompletedRequests(UUID volunteerId) {
        return volunteerRequestHistoryRepository.findByVolunteerIdAndStatusOrderByCompletedAtDescUpdatedAtDesc(
                volunteerId,
                RequestStatus.COMPLETED
        );
    }

    private Instant effectiveTime(Instant changedAt) {
        return changedAt == null ? Instant.now(clock) : changedAt;
    }
}
